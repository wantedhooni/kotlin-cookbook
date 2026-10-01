package com.example.trading.mockkrx.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MockMatchingEngineTest {
    private val engine = MockMatchingEngine(BigDecimal("70000"))

    @Test fun `quantity greater than one is partially filled first`() {
        engine.firstFillQuantity(10) shouldBe 5
        engine.firstFillQuantity(3) shouldBe 1
    }
}
