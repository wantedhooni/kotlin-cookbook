package com.example.trading.domain

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

fun interface BusinessDayCalendar {
    fun isBusinessDay(date: LocalDate): Boolean

    fun plusBusinessDays(tradeDate: LocalDate, days: Int): LocalDate {
        require(days >= 0)
        var cursor = tradeDate
        var added = 0
        while (added < days) {
            cursor = cursor.plusDays(1)
            if (isBusinessDay(cursor)) added++
        }
        return cursor
    }
}

class WeekendAndHolidayCalendar(private val holidays: Set<LocalDate> = emptySet()) : BusinessDayCalendar {
    override fun isBusinessDay(date: LocalDate): Boolean =
        date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) && date !in holidays
}

enum class SettlementStatus { SCHEDULED, SETTLED, OVERDUE }

data class SettlementObligation(
    val executionId: String,
    val accountId: String,
    val side: Side,
    val grossAmount: BigDecimal,
    val tradeDate: LocalDate,
    val settlementDate: LocalDate,
    var status: SettlementStatus = SettlementStatus.SCHEDULED,
    var shortageAmount: BigDecimal = BigDecimal.ZERO,
) {
    init {
        require(executionId.isNotBlank())
        require(accountId.isNotBlank())
        require(grossAmount > BigDecimal.ZERO)
        require(!settlementDate.isBefore(tradeDate))
        require(shortageAmount >= BigDecimal.ZERO)
    }

    fun settle() {
        status = SettlementStatus.SETTLED
        shortageAmount = BigDecimal.ZERO
    }

    fun overdue(shortage: BigDecimal) {
        require(shortage > BigDecimal.ZERO)
        status = SettlementStatus.OVERDUE
        shortageAmount = shortage
    }
}
