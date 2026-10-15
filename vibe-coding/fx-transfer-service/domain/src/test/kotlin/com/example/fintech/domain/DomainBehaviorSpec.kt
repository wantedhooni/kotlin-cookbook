package com.example.fintech.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency
import java.util.UUID

class DomainBehaviorSpec : BehaviorSpec({
    val krw = Currency.getInstance("KRW")

    Given("사용 가능 잔액이 100원인 원화 계좌가 주어졌을 때") {
        When("잔액보다 큰 금액을 출금하면") {
            val account = Account(UUID.randomUUID(), krw, BigDecimal("100"))
            Then("잔액은 변경되지 않고 출금이 거절된다") {
                shouldThrow<DomainException> {
                    account.debit(Money(BigDecimal("101"), krw))
                }
                account.balance shouldBe BigDecimal("100")
            }
        }
    }

    Given("두 개의 원화 계좌가 주어졌을 때") {
        When("한 계좌에서 다른 계좌로 25원을 이체하면") {
            val sender = Account(UUID.randomUUID(), krw, BigDecimal("100"))
            val receiver = Account(UUID.randomUUID(), krw, BigDecimal("50"))
            val amount = Money(BigDecimal("25"), krw)
            Then("두 계좌의 잔액 합계는 변하지 않는다") {
                sender.debit(amount).balance.add(receiver.credit(amount).balance) shouldBe BigDecimal("150")
            }
        }
    }

    Given("원화에서 달러로 환전하는 환율이 주어졌을 때") {
        When("1,000원을 달러로 환전하면") {
            val usd = Currency.getInstance("USD")
            val quote = FxQuote(krw, usd, BigDecimal("0.00072"), Instant.parse("2026-01-01T00:00:00Z"))
            Then("달러의 최소 화폐 단위에 맞춰 반올림한다") {
                quote.convert(Money(BigDecimal("1000"), krw)).amount shouldBe BigDecimal("0.72")
            }
        }
    }
})
