package com.example.trading.domain

import java.math.BigDecimal
import java.math.RoundingMode

class Position(
    val accountId: String,
    val symbol: String,
    quantity: Long = 0,
    averagePrice: BigDecimal = BigDecimal.ZERO,
    realizedPnl: BigDecimal = BigDecimal.ZERO,
) {
    var quantity: Long = quantity
        private set
    var averagePrice: BigDecimal = averagePrice
        private set
    var realizedPnl: BigDecimal = realizedPnl
        private set

    fun buy(executionQuantity: Long, executionPrice: BigDecimal) {
        require(executionQuantity > 0 && executionPrice > BigDecimal.ZERO)
        val oldCost = averagePrice.multiply(BigDecimal.valueOf(quantity))
        val newCost = executionPrice.multiply(BigDecimal.valueOf(executionQuantity))
        val newQuantity = quantity + executionQuantity
        averagePrice = oldCost.add(newCost).divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP)
        quantity = newQuantity
    }

    fun sell(executionQuantity: Long, executionPrice: BigDecimal) {
        require(executionQuantity > 0 && executionPrice > BigDecimal.ZERO)
        check(executionQuantity <= quantity) { "sell execution exceeds owned quantity" }
        val pnl = executionPrice.subtract(averagePrice).multiply(BigDecimal.valueOf(executionQuantity))
        realizedPnl = realizedPnl.add(pnl)
        quantity -= executionQuantity
        if (quantity == 0L) averagePrice = BigDecimal.ZERO
    }
}
