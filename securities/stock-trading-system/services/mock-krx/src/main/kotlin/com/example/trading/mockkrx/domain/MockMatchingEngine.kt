package com.example.trading.mockkrx.domain

import com.example.trading.event.KrxOrderRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class MockMatchingEngine(@Value("\${mock-krx.market-price:70000}") private val marketPrice: BigDecimal) {
    fun executionPrice(order: KrxOrderRequest): BigDecimal = order.price ?: marketPrice
    fun firstFillQuantity(total: Long): Long = if (total <= 1) total else maxOf(1, total / 2)
}
