package com.example.bank.domain

import java.math.BigDecimal
import java.util.UUID

class BankException(val code: String, override val message: String) : RuntimeException(message)
enum class AccountStatus { ACTIVE, CLOSED }
enum class EntrySide { DEBIT, CREDIT }
enum class Book { CUSTOMER, CLEARING }
data class Account(val id: Long, val bankCode: String, val accountNumber: String, val ownerName: String,
                   val balance: BigDecimal, val status: AccountStatus = AccountStatus.ACTIVE) {
    fun credit(amount: BigDecimal): Account { active(); return copy(balance = balance + Money.valid(amount)) }
    fun debit(amount: BigDecimal): Account {
        active(); val checked = Money.valid(amount)
        if (balance < checked) throw BankException("INSUFFICIENT_FUNDS", "Insufficient funds")
        return copy(balance = balance - checked)
    }
    private fun active() { if (status != AccountStatus.ACTIVE) throw BankException("ACCOUNT_CLOSED", "Account closed") }
}
object Money {
    fun valid(amount: BigDecimal): BigDecimal {
        if (amount.signum() <= 0 || amount.scale() > 2 || amount.precision() > 19)
            throw BankException("INVALID_AMOUNT", "Positive amount with up to two decimal places required")
        return amount.setScale(2)
    }
}
/** A journal balances when the sum of all signed posting amounts equals zero. */
data class Posting(val accountId: Long?, val book: Book, val side: EntrySide, val amount: BigDecimal) {
    val signed: BigDecimal get() = if (side == EntrySide.CREDIT) amount else amount.negate()
}
data class Journal(val referenceId: UUID, val postings: List<Posting>) {
    init {
        if (postings.size < 2 || postings.any { Money.valid(it.amount) != it.amount } ||
            postings.fold(BigDecimal.ZERO) { sum, p -> sum + p.signed }.compareTo(BigDecimal.ZERO) != 0)
            throw BankException("UNBALANCED_JOURNAL", "Journal must balance with at least two valid postings")
    }
}
