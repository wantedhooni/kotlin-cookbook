package com.example.bank.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.util.UUID

class JournalTest : BehaviorSpec({
    Given("동일 금액의 차변과 대변이 존재할 때") {
        When("원장을 생성하면") {
            Then("분개 합계가 0이다") {
                val journal = Journal(UUID.randomUUID(), listOf(
                    Posting(1, Book.CUSTOMER, EntrySide.DEBIT, BigDecimal("50.00")),
                    Posting(2, Book.CUSTOMER, EntrySide.CREDIT, BigDecimal("50.00"))
                ))
                journal.postings.sumOf { it.signed } shouldBe BigDecimal("0.00")
            }
        }
    }
    Given("차변과 대변 금액이 서로 다를 때") {
        When("원장을 생성하면") {
            Then("도메인 예외가 발생한다") {
                shouldThrow<BankException> {
                    Journal(UUID.randomUUID(), listOf(
                        Posting(1, Book.CUSTOMER, EntrySide.DEBIT, BigDecimal("50.00")),
                        Posting(2, Book.CUSTOMER, EntrySide.CREDIT, BigDecimal("40.00"))
                    ))
                }
            }
        }
    }
    Given("잔액이 3원인 계좌가 있을 때") {
        When("4원을 출금하면") {
            Then("잔액 부족 예외가 발생한다") {
                shouldThrow<BankException> {
                    Account(1, "BANK001", "100", "Owner", BigDecimal("3.00")).debit(BigDecimal("4.00"))
                }
            }
        }
    }
})
