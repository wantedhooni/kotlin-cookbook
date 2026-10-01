package com.example.trading.event

import com.example.trading.domain.MarketSession
import com.example.trading.domain.OrderType
import com.example.trading.domain.Side
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class OrderPlacedEvent(
    val eventId: UUID,
    val orderId: UUID,
    val accountId: String,
    val symbol: String,
    val side: Side,
    val orderType: OrderType,
    val quantity: Long,
    val price: BigDecimal?,
    val requestedAt: Instant,
)

data class OrderCancelRequestedEvent(val eventId: UUID, val orderId: UUID, val requestedAt: Instant)

data class OrderAcceptedEvent(
    val eventId: UUID,
    val orderId: UUID,
    val marketSession: MarketSession,
    val occurredAt: Instant,
)

data class OrderRejectedEvent(
    val eventId: UUID,
    val orderId: UUID,
    val reasonCode: String,
    val message: String,
    val occurredAt: Instant,
)

data class KrxOrderRequest(
    val eventId: UUID,
    val orderId: UUID,
    val accountId: String,
    val symbol: String,
    val side: Side,
    val orderType: OrderType,
    val quantity: Long,
    val price: BigDecimal?,
    val marketSession: MarketSession,
    val routedAt: Instant,
)

data class KrxCancelRequest(val eventId: UUID, val orderId: UUID, val routedAt: Instant)

data class KrxExecutionEvent(
    val eventId: UUID,
    val executionId: String,
    val orderId: UUID,
    val accountId: String,
    val symbol: String,
    val side: Side,
    val executionQuantity: Long,
    val executionPrice: BigDecimal,
    val occurredAt: Instant,
)

data class ExecutionBookedEvent(
    val eventId: UUID,
    val executionId: String,
    val orderId: UUID,
    val accountId: String,
    val symbol: String,
    val side: Side,
    val quantity: Long,
    val price: BigDecimal,
    val occurredAt: Instant,
)

data class OrderCanceledEvent(
    val eventId: UUID,
    val orderId: UUID,
    val canceledQuantity: Long,
    val occurredAt: Instant,
)

data class OrderCancelRejectedEvent(
    val eventId: UUID,
    val orderId: UUID,
    val message: String,
    val occurredAt: Instant,
)


data class OrderCorrectionRequestedEvent(
    val eventId: UUID,
    val correctionId: UUID,
    val orderId: UUID,
    val newPrice: BigDecimal,
    val requestedAt: Instant,
)

data class KrxCorrectionRequest(
    val eventId: UUID,
    val correctionId: UUID,
    val orderId: UUID,
    val newPrice: BigDecimal,
    val routedAt: Instant,
)

data class OrderCorrectedEvent(
    val eventId: UUID,
    val correctionId: UUID,
    val orderId: UUID,
    val oldPrice: BigDecimal,
    val newPrice: BigDecimal,
    val remainingQuantityAtAcceptance: Long,
    val occurredAt: Instant,
)

data class OrderCorrectionRejectedEvent(
    val eventId: UUID,
    val correctionId: UUID,
    val orderId: UUID,
    val message: String,
    val occurredAt: Instant,
)
