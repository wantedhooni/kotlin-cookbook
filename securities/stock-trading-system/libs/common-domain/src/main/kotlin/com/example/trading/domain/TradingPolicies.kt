package com.example.trading.domain

import java.math.BigDecimal

object TradingPolicies {
    fun validatePriceTick(orderType: OrderType, price: BigDecimal?) {
        if (orderType == OrderType.MARKET) return
        require(price != null)
        val tick = mockTickSize(price)
        require(price.remainder(tick).compareTo(BigDecimal.ZERO) == 0) {
            "price $price does not match mock tick size $tick"
        }
    }

    /** Educational Mock KRX tick policy. Do not treat this as the production KRX rule table. */
    fun mockTickSize(price: BigDecimal): BigDecimal = when {
        price < BigDecimal("2000") -> BigDecimal.ONE
        price < BigDecimal("5000") -> BigDecimal("5")
        price < BigDecimal("20000") -> BigDecimal("10")
        price < BigDecimal("50000") -> BigDecimal("50")
        price < BigDecimal("200000") -> BigDecimal("100")
        price < BigDecimal("500000") -> BigDecimal("500")
        else -> BigDecimal("1000")
    }

    fun isOrderTypeAllowed(session: MarketSession, type: OrderType): Boolean = when (session) {
        MarketSession.REGULAR -> true
        MarketSession.AFTER_HOURS, MarketSession.SINGLE_PRICE -> type == OrderType.LIMIT
        MarketSession.CLOSED -> false
    }
}
