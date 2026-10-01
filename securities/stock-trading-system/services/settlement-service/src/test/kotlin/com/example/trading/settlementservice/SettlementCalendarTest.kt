package com.example.trading.settlementservice
import com.example.trading.domain.WeekendAndHolidayCalendar
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
class SettlementCalendarTest { @Test fun `T plus 2 skips weekend and holiday`(){
    val cal=WeekendAndHolidayCalendar(setOf(LocalDate.of(2026,9,28)))
    cal.plusBusinessDays(LocalDate.of(2026,9,25),2) shouldBe LocalDate.of(2026,9,30)
} }
