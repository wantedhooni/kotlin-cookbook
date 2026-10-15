package com.example.trs.application

import com.example.trs.domain.*
import com.example.trs.repository.*
import com.example.trs.support.TestFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import java.math.BigDecimal
import java.util.Optional

class RefundServiceBehaviorSpec : BehaviorSpec({
    // Given/When/Then each creates isolated mocks; no scenario depends on execution order.
    fun subject(): Triple<RefundService, RefundDocumentRepository, Pair<RefundRepository, PayoutRepository>> {
        val docs = mockk<RefundDocumentRepository>()
        val refunds = mockk<RefundRepository>()
        val payouts = mockk<PayoutRepository>()
        return Triple(RefundService(docs, refunds, payouts), docs, refunds to payouts)
    }

    Given("정상 전표가 존재하고 아직 환급이 신청되지 않았을 때") {
        When("허용 가능한 금액으로 환급을 요청하면") {
            Then("REQUESTED 환급을 한 번 저장한다") {
                val (service, docs, repos) = subject()
                val (refunds, _) = repos
                every { docs.findById(10) } returns Optional.of(TestFixtures.document(id = 10))
                every { refunds.existsByDocumentId(10) } returns false
                every { refunds.save(any()) } answers { firstArg() }
                val result = service.request(10, BigDecimal("7000"))
                result.status shouldBe RefundStatus.REQUESTED
                result.amount shouldBe BigDecimal("7000")
                verify(exactly = 1) { refunds.save(match { it.status == RefundStatus.REQUESTED }) }
            }
        }
    }
    Given("전표를 조회할 수 없을 때") {
        When("환급을 요청하면") {
            Then("전표 없음 예외와 저장 0회를 확인한다") {
                val (service, docs, repos) = subject()
                val (refunds, _) = repos
                every { docs.findById(99) } returns Optional.empty()
                shouldThrow<NoSuchElementException> { service.request(99, BigDecimal("7000")) }
                verify { refunds wasNot Called }
            }
        }
    }
    Given("취소된 구매의 전표가 있을 때") {
        When("환급 요청을 하면") {
            Then("취소 구매를 거부하고 중복 조회와 저장을 하지 않는다") {
                val (service, docs, repos) = subject()
                val (refunds, _) = repos
                every { docs.findById(10) } returns Optional.of(
                    TestFixtures.document(id = 10, purchase = TestFixtures.purchase(status = PurchaseStatus.CANCELLED)))
                shouldThrow<IllegalStateException> { service.request(10, BigDecimal("7000")) }
                verify { refunds wasNot Called }
            }
        }
    }
    Given("환급 불가 금액") {
        listOf("0", "-1", "100001").forEach { invalid ->
            When("금액 $invalid 을 신청하면") {
                Then("저장하지 않고 IllegalArgumentException을 반환한다") {
                    val (service, docs, repos) = subject()
                    val (refunds, _) = repos
                    every { docs.findById(10) } returns Optional.of(TestFixtures.document(id = 10))
                    shouldThrow<IllegalArgumentException> { service.request(10, BigDecimal(invalid)) }
                    verify { refunds wasNot Called }
                }
            }
        }
    }
    Given("이미 환급 신청된 전표") {
        When("같은 전표로 다시 신청하면") {
            Then("중복 신청을 거부한다") {
                val (service, docs, repos) = subject()
                val (refunds, _) = repos
                every { docs.findById(10) } returns Optional.of(TestFixtures.document(id = 10))
                every { refunds.existsByDocumentId(10) } returns true
                shouldThrow<IllegalStateException> { service.request(10, BigDecimal("7000")) }
                verify(exactly = 0) { refunds.save(any()) }
            }
        }
    }
    Given("REQUESTED 환급이 존재하면") {
        When("승인하면") {
            Then("승인 상태로 바뀐다") {
                val (service, _, repos) = subject()
                val (refunds, _) = repos
                val refund = TestFixtures.refund(id = 5)
                every { refunds.findById(5) } returns Optional.of(refund)
                service.approve(5)
                refund.status shouldBe RefundStatus.APPROVED
                verify(exactly = 1) { refunds.findById(5) }
            }
        }
    }
    Given("환급 ID가 없을 때") {
        When("상세 조회를 요청하면") {
            Then("NotFound에 대응하는 예외를 발생시킨다") {
                val (service, _, repos) = subject()
                every { repos.first.findById(404) } returns Optional.empty()
                shouldThrow<NoSuchElementException> { service.get(404) }
            }
        }
    }
    Given("APPROVED 환급이 존재하면") {
        When("지급 준비를 요청하면") {
            Then("PROCESSING으로 바뀌고 고유 지급 키와 PENDING 지급 요청이 생성된다") {
                val (service, _, repos) = subject()
                val (refunds, payouts) = repos
                val refund = TestFixtures.refund(id = 5, status = RefundStatus.APPROVED)
                val captured = slot<Payout>()
                every { refunds.findById(5) } returns Optional.of(refund)
                every { payouts.save(capture(captured)) } answers { firstArg() }
                val key = service.preparePayout(5)
                key shouldStartWith "refund-5-"
                refund.status shouldBe RefundStatus.PROCESSING
                captured.captured.idempotencyKey shouldBe key
                captured.captured.amount shouldBe BigDecimal("7000")
                captured.captured.status shouldBe PayoutStatus.PENDING
                captured.captured.refund shouldBe refund
                verify(exactly = 1) { payouts.save(any()) }
            }
        }
    }
    Given("승인되지 않은 환급이 존재하면") {
        When("지급 준비를 요청하면") {
            Then("지급 요청을 저장하지 않는다") {
                val (service, _, repos) = subject()
                val (refunds, payouts) = repos
                every { refunds.findById(5) } returns Optional.of(TestFixtures.refund(id = 5))
                shouldThrow<IllegalStateException> { service.preparePayout(5) }
                verify { payouts wasNot Called }
            }
        }
    }
    Given("검색 조건이 전달되면") {
        When("상태와 가맹점으로 검색하면") {
            Then("Repository 검색에 전달하고 반환값을 유지한다") {
                val (service, _, repos) = subject()
                val expected = listOf(TestFixtures.refund(id = 1))
                every { repos.first.search(RefundStatus.REQUESTED, 33) } returns expected
                service.search(RefundStatus.REQUESTED, 33) shouldBe expected
                verify(exactly = 1) { repos.first.search(RefundStatus.REQUESTED, 33) }
            }
        }
    }
})
