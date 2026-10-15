package com.portfolio.payments.payment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PaymentServiceSpec : BehaviorSpec({
    val tx = mockk<PaymentTxService>()
    val van = mockk<VanClient>()
    val service = PaymentService(tx, van)

    Given("VAN 승인은 성공하지만 내부 결제 확정이 실패한 경우") {
        val command = PayCommand("seller-1", 100_000, "tok_card", "idem-1", true)
        val payment = Payment("pay-1", "seller-1", "idem-1", 100_000)
        every { tx.start(command) } returns payment
        every { van.authorize(any()) } returns VanAuthResponse(true, "van-1", "00")
        every { tx.approve("pay-1", "van-1", true) } throws IllegalStateException("db failure")
        every { van.cancel(any()) } returns Unit
        every { tx.markCanceled("pay-1", any()) } returns Unit

        When("결제를 수행하면") {
            val result = service.pay(command)
            Then("망취소를 수행해 외부 승인과 내부 상태의 불일치를 보상한다") {
                result.status shouldBe PaymentStatus.CANCELED
                result.compensated shouldBe true
                verify(exactly = 1) { van.cancel(match { it.vanTransactionId == "van-1" }) }
            }
        }
    }

    Given("같은 멱등키가 이미 승인된 경우") {
        val command = PayCommand("seller-1", 100_000, "tok_card", "idem-2")
        val payment = Payment("pay-2", "seller-1", "idem-2", 100_000).apply { approve("van-2") }
        every { tx.start(command) } returns payment
        When("재요청하면") {
            val result = service.pay(command)
            Then("VAN을 다시 호출하지 않고 기존 결과를 반환한다") {
                result.status shouldBe PaymentStatus.APPROVED
                verify(exactly = 0) { van.authorize(any()) }
            }
        }
    }
})
