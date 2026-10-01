package com.example.trading.domain

enum class Side { BUY, SELL }
enum class OrderType { LIMIT, MARKET }
enum class MarketSession { REGULAR, AFTER_HOURS, SINGLE_PRICE, CLOSED }
enum class OrderStatus {
    RECEIVED, ACCEPTED, PARTIALLY_FILLED, FILLED,
    CORRECTION_REQUESTED, CANCEL_REQUESTED, CANCELED, REJECTED
}
