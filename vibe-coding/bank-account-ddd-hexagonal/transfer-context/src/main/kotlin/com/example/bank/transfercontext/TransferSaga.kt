package com.example.bank.transfercontext

import java.math.BigDecimal
import java.util.UUID

/** Transfer context owns the saga state machine, not account storage. */
enum class SagaState { REQUESTED, DEBITED, COMPLETED, COMPENSATING, COMPENSATED, MANUAL_REVIEW }
data class TransferSaga(val requestId: UUID, val source: Long, val target: Long,
                        val amount: BigDecimal, val state: SagaState = SagaState.REQUESTED) {
    init { require(source != target && amount.signum() > 0 && amount.scale() <= 2) }
    fun afterDebit(): TransferSaga { require(state == SagaState.REQUESTED); return copy(state = SagaState.DEBITED) }
    fun afterCredit(): TransferSaga { require(state == SagaState.DEBITED); return copy(state = SagaState.COMPLETED) }
    fun beginCompensation(): TransferSaga { require(state == SagaState.DEBITED); return copy(state = SagaState.COMPENSATING) }
    fun compensated(): TransferSaga { require(state == SagaState.COMPENSATING); return copy(state = SagaState.COMPENSATED) }
    fun manualReview(): TransferSaga { require(state == SagaState.COMPENSATING); return copy(state = SagaState.MANUAL_REVIEW) }
}

/** Adapters MUST be idempotent per request ID. */
interface TransferSagaPort {
    fun debit(requestId: UUID, account: Long, amount: BigDecimal)
    fun credit(requestId: UUID, account: Long, amount: BigDecimal)
    fun refund(requestId: UUID, account: Long, amount: BigDecimal)
    fun save(state: TransferSaga)
}

/** Compensating saga for eventual-consistency mode; regular same-DB transfers remain atomic. */
class TransferSagaCoordinator(private val port: TransferSagaPort) {
    fun execute(request: TransferSaga): TransferSaga {
        require(request.state == SagaState.REQUESTED)
        port.debit(request.requestId, request.source, request.amount)
        var saga = request.afterDebit().also(port::save)
        try {
            port.credit(request.requestId, request.target, request.amount)
            saga = saga.afterCredit().also(port::save)
        } catch (failure: Exception) {
            saga = saga.beginCompensation().also(port::save)
            try {
                port.refund(request.requestId, request.source, request.amount)
                saga = saga.compensated().also(port::save)
            } catch (refundFailure: Exception) {
                saga = saga.manualReview().also(port::save)
            }
        }
        return saga
    }
}
