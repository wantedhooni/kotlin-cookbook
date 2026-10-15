package com.example.trs.domain

import com.example.trs.support.TestFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class RefundBehaviorSpec : BehaviorSpec({
    Given("0원 이하의 환급액") {
        When("환급을 생성하면") {
            Then("0원은 거부된다") {
                shouldThrow<IllegalArgumentException> { TestFixtures.refund(amount = "0") }
            }
            Then("음수 금액도 거부된다") {
                shouldThrow<IllegalArgumentException> { TestFixtures.refund(amount = "-100") }
            }
        }
    }
    Given("신청 상태의 환급") {
        When("승인하면") {
            Then("APPROVED로 전이된다") {
                val refund = TestFixtures.refund()
                refund.approve()
                refund.status shouldBe RefundStatus.APPROVED
            }
        }
        When("즉시 지급·완료·실패 처리하면") {
            Then("모든 잘못된 전이는 거부되고 상태가 유지된다") {
                val refund = TestFixtures.refund()
                shouldThrow<IllegalStateException> { refund.startPayout() }
                shouldThrow<IllegalStateException> { refund.complete() }
                shouldThrow<IllegalStateException> { refund.fail() }
                refund.status shouldBe RefundStatus.REQUESTED
            }
        }
        When("취소하면") {
            Then("CANCELLED가 되고 재취소도 실패한다") {
                val refund = TestFixtures.refund()
                refund.cancel()
                refund.status shouldBe RefundStatus.CANCELLED
                shouldThrow<IllegalStateException> { refund.cancel() }
            }
        }
    }
    Given("승인된 환급") {
        When("지급을 시작하면") {
            Then("PROCESSING이 된다") {
                val refund = TestFixtures.refund(status = RefundStatus.APPROVED)
                refund.startPayout()
                refund.status shouldBe RefundStatus.PROCESSING
            }
        }
        When("취소를 시도하면") {
            Then("승인 취소는 허용되지 않는다") {
                val refund = TestFixtures.refund(status = RefundStatus.APPROVED)
                shouldThrow<IllegalStateException> { refund.cancel() }
            }
        }
    }
    Given("지급 처리 중인 환급") {
        When("지급 성공 결과를 받으면") {
            Then("COMPLETED가 되고 중복 완료는 차단된다") {
                val refund = TestFixtures.refund(status = RefundStatus.PROCESSING)
                refund.complete()
                refund.status shouldBe RefundStatus.COMPLETED
                shouldThrow<IllegalStateException> { refund.complete() }
            }
        }
        When("지급 실패 결과를 받으면") {
            Then("FAILED가 되고 완료 처리할 수 없다") {
                val refund = TestFixtures.refund(status = RefundStatus.PROCESSING)
                refund.fail()
                refund.status shouldBe RefundStatus.FAILED
                shouldThrow<IllegalStateException> { refund.complete() }
            }
        }
    }
    Given("이미 종료된 환급") {
        listOf(RefundStatus.COMPLETED, RefundStatus.FAILED, RefundStatus.CANCELLED).forEach { terminal ->
            When("상태가 $terminal 일 때 다시 승인하면") {
                Then("예외가 발생한다") {
                    val refund = TestFixtures.refund(status = terminal)
                    shouldThrow<IllegalStateException> { refund.approve() }
                    refund.status shouldBe terminal
                }
            }
        }
    }
})
