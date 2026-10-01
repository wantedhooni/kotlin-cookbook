package com.example.trading.domain

import java.math.BigDecimal
import java.math.RoundingMode

class TradingAccount(
    val accountId: String,
    cashBalance: BigDecimal = BigDecimal.ZERO,
    reservedCash: BigDecimal = BigDecimal.ZERO,
    creditLimit: BigDecimal = BigDecimal.ZERO,
    usedCredit: BigDecimal = BigDecimal.ZERO,
    reservedCredit: BigDecimal = BigDecimal.ZERO,
    settlementCashHold: BigDecimal = BigDecimal.ZERO,
    settlementCreditHold: BigDecimal = BigDecimal.ZERO,
    pendingSettlementReceivable: BigDecimal = BigDecimal.ZERO,
    overdueAmount: BigDecimal = BigDecimal.ZERO,
) {
    var cashBalance: BigDecimal = cashBalance; private set
    var reservedCash: BigDecimal = reservedCash; private set
    var creditLimit: BigDecimal = creditLimit; private set
    var usedCredit: BigDecimal = usedCredit; private set
    var reservedCredit: BigDecimal = reservedCredit; private set
    var settlementCashHold: BigDecimal = settlementCashHold; private set
    var settlementCreditHold: BigDecimal = settlementCreditHold; private set
    var pendingSettlementReceivable: BigDecimal = pendingSettlementReceivable; private set
    var overdueAmount: BigDecimal = overdueAmount; private set

    init {
        require(accountId.isNotBlank()) { "accountId must not be blank" }
        listOf(cashBalance, reservedCash, creditLimit, usedCredit, reservedCredit, settlementCashHold,
            settlementCreditHold, pendingSettlementReceivable, overdueAmount).forEach {
            require(it >= BigDecimal.ZERO) { "account money values must not be negative" }
        }
        require(reservedCash + settlementCashHold <= cashBalance) { "cash reservations/holds cannot exceed cash balance" }
        require(usedCredit + reservedCredit + settlementCreditHold <= creditLimit) { "credit usage cannot exceed credit limit" }
    }

    val availableCash: BigDecimal get() = cashBalance - reservedCash - settlementCashHold
    val availableCredit: BigDecimal get() = creditLimit - usedCredit - reservedCredit - settlementCreditHold
    val orderableAmount: BigDecimal get() = availableCash + availableCredit
    val pendingSettlementPayable: BigDecimal get() = settlementCashHold + settlementCreditHold

    fun reserveForBuy(notional: BigDecimal, marginRate: BigDecimal): FundsReservation {
        require(notional > BigDecimal.ZERO) { "notional must be positive" }
        require(marginRate > BigDecimal.ZERO && marginRate <= BigDecimal.ONE) { "marginRate must be in (0, 1]" }
        val cashRequired = notional.multiply(marginRate).setScale(4, RoundingMode.CEILING)
        val creditRequired = notional.subtract(cashRequired).max(BigDecimal.ZERO)
        check(availableCash >= cashRequired) { "insufficient cash for margin: available=$availableCash required=$cashRequired" }
        check(availableCredit >= creditRequired) { "insufficient credit: available=$availableCredit required=$creditRequired" }
        reservedCash += cashRequired
        reservedCredit += creditRequired
        return FundsReservation(cashRequired, creditRequired)
    }

    /** Converts an order-time reservation into a D+2 settlement hold. */
    fun commitBuyForSettlement(reserved: FundsReservation, actualNotional: BigDecimal, marginRate: BigDecimal): FundsReservation {
        require(actualNotional > BigDecimal.ZERO)
        val actualCash = actualNotional.multiply(marginRate).setScale(4, RoundingMode.CEILING)
        val actualCredit = actualNotional.subtract(actualCash).max(BigDecimal.ZERO)
        check(reserved.cash >= actualCash && reserved.credit >= actualCredit) { "actual execution exceeds protected reservation" }
        check(reservedCash >= reserved.cash && reservedCredit >= reserved.credit) { "reservation already consumed" }
        reservedCash -= reserved.cash
        reservedCredit -= reserved.credit
        settlementCashHold += actualCash
        settlementCreditHold += actualCredit
        return FundsReservation(actualCash, actualCredit)
    }

    fun settleBuy(hold: FundsReservation): SettlementResult {
        check(settlementCashHold >= hold.cash && settlementCreditHold >= hold.credit) { "settlement hold is missing" }
        settlementCashHold -= hold.cash
        settlementCreditHold -= hold.credit
        val payableCash = hold.cash
        val paidCash = payableCash.min(cashBalance)
        cashBalance -= paidCash
        usedCredit += hold.credit
        val shortage = payableCash - paidCash
        if (shortage > BigDecimal.ZERO) overdueAmount += shortage
        return SettlementResult(shortage == BigDecimal.ZERO, shortage)
    }

    fun registerSellReceivable(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO)
        pendingSettlementReceivable += amount
    }

    fun settleSellReceivable(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO)
        check(pendingSettlementReceivable >= amount) { "pending sell receivable is insufficient" }
        pendingSettlementReceivable -= amount
        cashBalance += amount
    }

    fun release(reservation: FundsReservation) {
        check(reservedCash >= reservation.cash && reservedCredit >= reservation.credit) { "reservation already released" }
        reservedCash -= reservation.cash
        reservedCredit -= reservation.credit
    }

    fun repayCredit(amount: BigDecimal) {
        require(amount > BigDecimal.ZERO)
        check(availableCash >= amount) { "insufficient available cash to repay credit" }
        val repayment = amount.min(usedCredit)
        cashBalance -= repayment
        usedCredit -= repayment
    }
}

data class SettlementResult(val settled: Boolean, val shortageAmount: BigDecimal)

data class FundsReservation(val cash: BigDecimal, val credit: BigDecimal) {
    init { require(cash >= BigDecimal.ZERO && credit >= BigDecimal.ZERO) }
    val total: BigDecimal get() = cash + credit
}

object MarginPolicy {
    fun validateRate(rate: BigDecimal) {
        require(rate > BigDecimal.ZERO && rate <= BigDecimal.ONE) { "margin rate must be in (0,1]" }
    }

    fun protectedUnitPrice(orderType: OrderType, limitPrice: BigDecimal?, marketReferencePrice: BigDecimal, marketBufferRate: BigDecimal): BigDecimal {
        require(marketReferencePrice > BigDecimal.ZERO)
        require(marketBufferRate >= BigDecimal.ZERO)
        return when (orderType) {
            OrderType.LIMIT -> requireNotNull(limitPrice)
            OrderType.MARKET -> marketReferencePrice.multiply(BigDecimal.ONE + marketBufferRate).setScale(4, RoundingMode.CEILING)
        }
    }
}
