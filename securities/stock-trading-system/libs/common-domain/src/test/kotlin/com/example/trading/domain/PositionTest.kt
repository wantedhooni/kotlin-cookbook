package com.example.trading.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PositionTest {
    @Test
    fun `weighted average price is recalculated on buy`() {
        val p = Position("A-1", "005930")
        p.buy(10, BigDecimal("100"))
        p.buy(10, BigDecimal("200"))
        p.quantity shouldBe 20
        p.averagePrice shouldBe BigDecimal("150.0000")
    }

    @Test
    fun `sell reduces quantity and accumulates realized pnl`() {
        val p = Position("A-1", "005930", 10, BigDecimal("100"))
        p.sell(4, BigDecimal("120"))
        p.quantity shouldBe 6
        p.realizedPnl shouldBe BigDecimal("80")
    }
}
