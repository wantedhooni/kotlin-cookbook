package com.example.bank.accountcontext

import java.math.BigDecimal

/** Account bounded context: balance is modified only through its aggregate. */
data class AccountAggregate(val id: Long, val bankCode: String, val ownerSubject: String,
                            val balance: BigDecimal, val active: Boolean = true) {
    fun credit(value: BigDecimal): AccountAggregate { validate(value); return copy(balance = balance + value) }
    fun debit(value: BigDecimal): AccountAggregate {
        validate(value)
        require(balance >= value) { "Insufficient funds" }
        return copy(balance = balance - value)
    }
    private fun validate(value: BigDecimal) { check(active); require(value.signum() > 0 && value.scale() <= 2) }
}
interface AccountGateway { fun find(id: Long): AccountAggregate?; fun save(aggregate: AccountAggregate) }
