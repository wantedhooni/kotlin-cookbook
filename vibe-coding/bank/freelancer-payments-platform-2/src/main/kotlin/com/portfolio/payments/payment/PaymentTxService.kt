package com.portfolio.payments.payment

import com.portfolio.payments.common.DomainException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PaymentTxService(private val repository: PaymentRepository) {

    @Transactional
    fun start(command: PayCommand): Payment {
        repository.findByIdempotencyKey(command.idempotencyKey)?.let { return it }
        return repository.saveAndFlush(
            Payment(
                id = UUID.randomUUID().toString(),
                sellerId = command.sellerId,
                idempotencyKey = command.idempotencyKey,
                amount = command.amount,
            )
        )
    }

    @Transactional(readOnly = true)
    fun findByIdempotencyKey(idempotencyKey: String): Payment? = repository.findByIdempotencyKey(idempotencyKey)

    @Transactional
    fun approve(paymentId: String, vanTxId: String, simulateInternalFailure: Boolean): Payment {
        val payment = repository.findById(paymentId).orElseThrow { DomainException("payment not found") }
        if (simulateInternalFailure) throw IllegalStateException("simulated DB/app failure after VAN approval")
        payment.approve(vanTxId)
        return payment
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markCanceled(paymentId: String, reason: String) {
        repository.findById(paymentId).orElseThrow().cancel(reason)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(paymentId: String, reason: String) {
        repository.findById(paymentId).orElseThrow().fail(reason)
    }
}
