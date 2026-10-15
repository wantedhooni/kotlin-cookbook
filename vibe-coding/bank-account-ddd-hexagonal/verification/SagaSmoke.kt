import com.example.bank.transfercontext.*
import java.math.BigDecimal
import java.util.UUID

class Fake(private val failCredit: Boolean = false, private val failRefund: Boolean = false) : TransferSagaPort {
    var refunds = 0
    override fun debit(requestId: UUID, account: Long, amount: BigDecimal) {}
    override fun credit(requestId: UUID, account: Long, amount: BigDecimal) { if (failCredit) error("credit failed") }
    override fun refund(requestId: UUID, account: Long, amount: BigDecimal) { refunds++; if (failRefund) error("refund failed") }
    override fun save(state: TransferSaga) {}
}
fun main() {
    fun run(failCredit: Boolean, failRefund: Boolean, expected: SagaState) {
        val port = Fake(failCredit, failRefund)
        val result = TransferSagaCoordinator(port).execute(TransferSaga(UUID.randomUUID(), 1, 2, BigDecimal("20.00")))
        check(result.state == expected) { "Expected $expected but was ${result.state}" }
        check(port.refunds == if (failCredit) 1 else 0)
        println("PASS: $expected")
    }
    run(false, false, SagaState.COMPLETED)
    run(true, false, SagaState.COMPENSATED)
    run(true, true, SagaState.MANUAL_REVIEW)
}
