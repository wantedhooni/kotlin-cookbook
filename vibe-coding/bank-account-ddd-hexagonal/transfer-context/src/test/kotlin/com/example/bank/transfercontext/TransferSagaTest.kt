package com.example.bank.transfercontext

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.util.UUID

class TransferSagaTest : BehaviorSpec({
    Given("수취 계좌 입금이 실패할 때") {
        When("사가 보상 트랜잭션을 실행하면") {
            Then("출금 금액이 환불되고 보상 완료 상태가 된다") {
                val port = FakePort(failCredit = true)
                val result = TransferSagaCoordinator(port).execute(TransferSaga(UUID.randomUUID(), 1, 2, BigDecimal("20.00")))
                result.state shouldBe SagaState.COMPENSATED
                port.refunds shouldBe 1
            }
        }
    }
    Given("수취 계좌 입금과 환불 모두 실패할 때") {
        When("사가를 실행하면") {
            Then("수동 검토 필요 상태로 전환된다") {
                val port = FakePort(failCredit = true, failRefund = true)
                val result = TransferSagaCoordinator(port).execute(TransferSaga(UUID.randomUUID(), 1, 2, BigDecimal("20.00")))
                result.state shouldBe SagaState.MANUAL_REVIEW
            }
        }
    }
    Given("출금·입금이 정상 처리될 때") {
        When("사가를 실행하면") {
            Then("정상 완료 상태가 된다") {
                val result = TransferSagaCoordinator(FakePort()).execute(TransferSaga(UUID.randomUUID(), 1, 2, BigDecimal("20.00")))
                result.state shouldBe SagaState.COMPLETED
            }
        }
    }
}) {
    private class FakePort(val failCredit: Boolean = false, val failRefund: Boolean = false) : TransferSagaPort {
        var refunds = 0
        override fun debit(requestId: UUID, account: Long, amount: BigDecimal) {}
        override fun credit(requestId: UUID, account: Long, amount: BigDecimal) { if (failCredit) error("credit failed") }
        override fun refund(requestId: UUID, account: Long, amount: BigDecimal) { refunds++; if (failRefund) error("refund failed") }
        override fun save(state: TransferSaga) {}
    }
}
