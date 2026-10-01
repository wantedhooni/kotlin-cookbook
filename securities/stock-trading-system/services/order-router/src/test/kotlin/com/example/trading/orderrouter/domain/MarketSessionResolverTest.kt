package com.example.trading.orderrouter.domain

import com.example.trading.domain.MarketSession
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.*

class MarketSessionResolverTest {
    @Test
    fun `regular session is resolved in auto mode`() {
        val instant = ZonedDateTime.of(2026, 9, 23, 10, 0, 0, 0, ZoneId.of("Asia/Seoul")).toInstant()
        MarketSessionResolver(Clock.fixed(instant, ZoneId.of("Asia/Seoul")), "AUTO").current() shouldBe MarketSession.REGULAR
    }
}
