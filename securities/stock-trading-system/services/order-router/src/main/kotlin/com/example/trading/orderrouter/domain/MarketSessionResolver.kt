package com.example.trading.orderrouter.domain

import com.example.trading.domain.MarketSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.*

@Component
class MarketSessionResolver(
    private val clock: Clock,
    @Value("\${trading.market-session-override:REGULAR}") private val override: String,
) {
    fun current(): MarketSession {
        if (!override.equals("AUTO", ignoreCase = true)) return MarketSession.valueOf(override.uppercase())
        val now = ZonedDateTime.now(clock)
        if (now.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) return MarketSession.CLOSED
        val time = now.toLocalTime()
        return when {
            time >= LocalTime.of(8, 30) && time < LocalTime.of(9, 0) -> MarketSession.SINGLE_PRICE
            time >= LocalTime.of(9, 0) && time <= LocalTime.of(15, 30) -> MarketSession.REGULAR
            time >= LocalTime.of(15, 40) && time <= LocalTime.of(18, 0) -> MarketSession.AFTER_HOURS
            else -> MarketSession.CLOSED
        }
    }
}
