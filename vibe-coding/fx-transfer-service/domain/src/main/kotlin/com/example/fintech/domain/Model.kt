package com.example.fintech.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Currency
import java.util.UUID

class DomainException(message: String) : RuntimeException(message)

data class Money(val amount: BigDecimal, val currency: Currency) {
    init { require(amount.signum() >= 0) { "Money must not be negative" } }
    fun positive(): Money { if (amount.signum() <= 0) throw DomainException("Amount must be positive"); return this }
}

data class Account(val id: UUID, val currency: Currency, val balance: BigDecimal) {
    fun debit(amount: Money): Account {
        verifyCurrency(amount)
        amount.positive()
        if (balance < amount.amount) throw DomainException("Insufficient funds")
        return copy(balance = balance.subtract(amount.amount))
    }
    fun credit(amount: Money): Account {
        verifyCurrency(amount)
        amount.positive()
        return copy(balance = balance.add(amount.amount))
    }
    private fun verifyCurrency(amount: Money) {
        if (amount.currency != currency) throw DomainException("Account currency mismatch")
    }
}

data class FxQuote(val base: Currency, val counter: Currency, val rate: BigDecimal, val observedAt: Instant) {
    init { require(rate.signum() > 0) { "Rate must be positive" } }
    fun convert(amount: Money): Money {
        if (amount.currency != base) throw DomainException("Quote currency mismatch")
        val scale = counter.defaultFractionDigits.coerceAtLeast(0)
        return Money(amount.amount.multiply(rate).setScale(scale, RoundingMode.HALF_EVEN), counter)
    }
}

data class TransferReceipt(val id: UUID, val sourceAccountId: UUID, val targetAccountId: UUID,
    val amount: Money, val createdAt: Instant)
data class ExchangeReceipt(val id: UUID, val sourceAccountId: UUID, val targetAccountId: UUID,
    val source: Money, val target: Money, val rate: BigDecimal, val createdAt: Instant)
