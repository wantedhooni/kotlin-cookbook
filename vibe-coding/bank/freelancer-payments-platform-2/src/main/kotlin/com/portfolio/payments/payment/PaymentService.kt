package com.portfolio.payments.payment

import com.portfolio.payments.common.DomainException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

data class PayCommand(
    val sellerId: String,
    val amount: Long,
    val cardToken: String,
    val idempotencyKey: String,
    val simulateInternalFailureAfterVanApproval: Boolean = false,
)

data class PaymentResult(
    val paymentId: String,
    val status: PaymentStatus,
    val vanTransactionId: String?,
    val compensated: Boolean,
)

@Service
class PaymentService(
    private val tx: PaymentTxService,
    private val vanClient: VanClient,
) {
    fun pay(command: PayCommand): PaymentResult {
        require(command.amount > 0) { "amount must be positive" }
        val payment = try {
            tx.start(command)
        } catch (ex: DataIntegrityViolationException) {
            // Concurrent requests can pass the pre-check together. DB unique constraint is the final guard.
            tx.findByIdempotencyKey(command.idempotencyKey) ?: throw ex
        }
        if (payment.status != PaymentStatus.REQUESTED) {
            return PaymentResult(payment.id, payment.status, payment.vanTransactionId, false)
        }

        val van = try {
            vanClient.authorize(VanAuthRequest(payment.id, command.amount, command.cardToken))
        } catch (ex: Exception) {
            tx.markFailed(payment.id, "VAN_NETWORK_ERROR")
            throw DomainException("VAN authorization network error")
        }

        if (!van.approved || van.vanTransactionId == null) {
            tx.markFailed(payment.id, "VAN_DECLINED:${van.code}")
            return PaymentResult(payment.id, PaymentStatus.FAILED, null, false)
        }

        return try {
            val approved = tx.approve(payment.id, van.vanTransactionId, command.simulateInternalFailureAfterVanApproval)
            PaymentResult(approved.id, approved.status, approved.vanTransactionId, false)
        } catch (ex: Exception) {
            // VAN 승인 성공 이후 내부 확정 실패: 승인 원복을 위해 망취소(compensation) 수행.
            try {
                vanClient.cancel(VanCancelRequest(payment.id, van.vanTransactionId, "INTERNAL_CONFIRM_FAILED"))
                tx.markCanceled(payment.id, "NETWORK_CANCEL_COMPLETED")
            } catch (cancelEx: Exception) {
                tx.markFailed(payment.id, "NETWORK_CANCEL_REQUIRED")
                throw DomainException("approval succeeded but network cancellation failed; manual reconciliation required")
            }
            PaymentResult(payment.id, PaymentStatus.CANCELED, van.vanTransactionId, true)
        }
    }
}
