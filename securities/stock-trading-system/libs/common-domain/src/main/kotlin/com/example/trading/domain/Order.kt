package com.example.trading.domain

import java.math.BigDecimal
import java.util.UUID

class Order(
    val id: UUID,
    val accountId: String,
    val symbol: String,
    val side: Side,
    val orderType: OrderType,
    val quantity: Long,
    price: BigDecimal?,
    filledQuantity: Long = 0,
    status: OrderStatus = OrderStatus.RECEIVED,
) {
    var price: BigDecimal? = price
        private set
    var filledQuantity: Long = filledQuantity
        private set
    var status: OrderStatus = status
        private set

    init {
        require(accountId.isNotBlank()) { "accountId must not be blank" }
        require(symbol.matches(Regex("\\d{6}"))) { "symbol must be a 6-digit Korean stock code" }
        require(quantity > 0) { "quantity must be greater than zero" }
        require(filledQuantity in 0..quantity) { "filledQuantity must be between 0 and quantity" }
        when (orderType) {
            OrderType.LIMIT -> require(price != null && price > BigDecimal.ZERO) { "LIMIT order requires a positive price" }
            OrderType.MARKET -> require(price == null) { "MARKET order price must be null" }
        }
    }

    val remainingQuantity: Long get() = quantity - filledQuantity
    val isTerminal: Boolean get() = status in setOf(OrderStatus.FILLED, OrderStatus.CANCELED, OrderStatus.REJECTED)

    fun accept() {
        if (status == OrderStatus.RECEIVED) status = OrderStatus.ACCEPTED
    }

    /**
     * Demo correction rule: price-only correction for LIMIT orders.
     * BUY corrections may only lower/equal the price so that an already-reserved buy amount is never exceeded.
     * SELL corrections can move either direction because sell reservation is quantity based.
     */
    fun requestCorrection(newPrice: BigDecimal) {
        check(orderType == OrderType.LIMIT) { "only LIMIT orders can be corrected" }
        check(!isTerminal) { "terminal order cannot be corrected" }
        check(status != OrderStatus.CANCEL_REQUESTED) { "cancel-requested order cannot be corrected" }
        check(status != OrderStatus.CORRECTION_REQUESTED) { "correction is already requested" }
        check(remainingQuantity > 0) { "fully filled order cannot be corrected" }
        require(newPrice > BigDecimal.ZERO) { "new price must be positive" }
        if (side == Side.BUY) check(newPrice <= requireNotNull(price)) {
            "BUY correction cannot raise price above reserved price in this demo"
        }
        status = OrderStatus.CORRECTION_REQUESTED
    }

    fun correctionAccepted(newPrice: BigDecimal) {
        if (status == OrderStatus.REJECTED) return
        // The acceptance event and the final execution are on different Kafka topics.
        // If the execution is observed first, keep the terminal state but still persist the accepted corrected price.
        if (status in setOf(OrderStatus.FILLED, OrderStatus.CANCELED)) {
            price = newPrice
            return
        }
        check(status == OrderStatus.CORRECTION_REQUESTED) { "order is not waiting for correction" }
        price = newPrice
        status = if (filledQuantity > 0) OrderStatus.PARTIALLY_FILLED else OrderStatus.ACCEPTED
    }

    fun correctionRejected() {
        if (isTerminal) return
        if (status == OrderStatus.CORRECTION_REQUESTED) {
            status = if (filledQuantity > 0) OrderStatus.PARTIALLY_FILLED else OrderStatus.ACCEPTED
        }
    }

    fun requestCancel() {
        check(!isTerminal) { "terminal order cannot be canceled" }
        check(status != OrderStatus.CORRECTION_REQUESTED) { "correction-requested order cannot be canceled until resolved" }
        check(remainingQuantity > 0) { "fully filled order cannot be canceled" }
        status = OrderStatus.CANCEL_REQUESTED
    }

    fun cancel() {
        check(status != OrderStatus.FILLED) { "filled order cannot be canceled" }
        status = OrderStatus.CANCELED
    }

    fun cancelRejected() {
        if (status != OrderStatus.CANCEL_REQUESTED) return
        status = if (filledQuantity > 0) OrderStatus.PARTIALLY_FILLED else OrderStatus.ACCEPTED
    }

    fun reject() {
        if (!isTerminal) status = OrderStatus.REJECTED
    }

    fun applyExecution(executionQuantity: Long) {
        require(executionQuantity > 0) { "execution quantity must be positive" }
        check(status !in setOf(OrderStatus.REJECTED, OrderStatus.FILLED)) {
            "execution cannot be applied to $status order"
        }
        check(executionQuantity <= remainingQuantity) { "execution quantity exceeds remaining order quantity" }
        val keepPendingCorrection = status == OrderStatus.CORRECTION_REQUESTED
        val canceledBeforeExecutionArrived = status == OrderStatus.CANCELED
        filledQuantity += executionQuantity
        status = when {
            canceledBeforeExecutionArrived -> OrderStatus.CANCELED
            filledQuantity == quantity -> OrderStatus.FILLED
            keepPendingCorrection -> OrderStatus.CORRECTION_REQUESTED
            else -> OrderStatus.PARTIALLY_FILLED
        }
    }
}
