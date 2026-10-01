package com.example.trading.event

object Topics {
    const val ORDER_PLACED = "trading.orders.placed"
    const val ORDER_CANCEL_REQUESTED = "trading.orders.cancel-requested"
    const val ORDER_CORRECTION_REQUESTED = "trading.orders.correction-requested"
    const val ORDER_ACCEPTED = "trading.orders.accepted"
    const val ORDER_REJECTED = "trading.orders.rejected"
    const val ORDER_CORRECTED = "trading.orders.corrected"
    const val ORDER_CORRECTION_REJECTED = "trading.orders.correction-rejected"
    const val KRX_ORDER_REQUEST = "trading.krx.orders"
    const val KRX_CANCEL_REQUEST = "trading.krx.cancel-requests"
    const val KRX_CORRECTION_REQUEST = "trading.krx.correction-requests"
    const val KRX_EXECUTION = "trading.krx.executions"
    const val ORDER_CANCELED = "trading.orders.canceled"
    const val ORDER_CANCEL_REJECTED = "trading.orders.cancel-rejected"
    const val EXECUTION_BOOKED = "trading.executions.booked"
}
