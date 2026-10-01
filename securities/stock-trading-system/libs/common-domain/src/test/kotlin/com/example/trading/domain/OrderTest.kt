package com.example.trading.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class OrderTest {
    @Test
    fun `partial then full execution changes order state`() {
        val order = Order(UUID.randomUUID(), "A-1", "005930", Side.BUY, OrderType.LIMIT, 10, BigDecimal("70000"))
        order.accept()
        order.applyExecution(4)
        order.status shouldBe OrderStatus.PARTIALLY_FILLED
        order.remainingQuantity shouldBe 6
        order.applyExecution(6)
        order.status shouldBe OrderStatus.FILLED
        order.remainingQuantity shouldBe 0
    }

    @Test
    fun `execution exceeding remaining quantity is rejected`() {
        val order = Order(UUID.randomUUID(), "A-1", "005930", Side.BUY, OrderType.LIMIT, 3, BigDecimal("70000"))
        assertThrows<IllegalStateException> { order.applyExecution(4) }
    }
    @Test
    fun `late execution after cancel updates filled quantity but keeps canceled state`() {
        val order = Order(UUID.randomUUID(), "A-1", "005930", Side.BUY, OrderType.LIMIT, 10, BigDecimal("70000"))
        order.accept()
        order.requestCancel()
        order.cancel()
        order.applyExecution(5)
        order.status shouldBe OrderStatus.CANCELED
        order.filledQuantity shouldBe 5
        order.remainingQuantity shouldBe 5
    }


    @Test
    fun `keeps correction pending when execution races before correction ack`() {
        val order = Order(UUID.randomUUID(), "A-1", "005930", Side.BUY, OrderType.LIMIT, 10, BigDecimal("70000"))
        order.accept()
        order.requestCorrection(BigDecimal("69000"))
        order.applyExecution(4)
        order.status shouldBe OrderStatus.CORRECTION_REQUESTED
        order.filledQuantity shouldBe 4
        order.correctionAccepted(BigDecimal("69000"))
        order.status shouldBe OrderStatus.PARTIALLY_FILLED
        order.price shouldBe BigDecimal("69000")
    }
}
