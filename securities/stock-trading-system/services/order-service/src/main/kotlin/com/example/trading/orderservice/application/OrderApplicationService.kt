package com.example.trading.orderservice.application

import com.example.trading.domain.*
import com.example.trading.event.*
import com.example.trading.orderservice.adapter.out.persistence.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

interface SellReservationPort {
    fun reserve(orderId: UUID, accountId: String, symbol: String, quantity: Long)
    fun release(orderId: UUID)
}
interface BuyReservationPort {
    fun reserve(orderId: UUID, accountId: String, symbol: String, orderType: OrderType, quantity: Long, price: BigDecimal?)
    fun release(orderId: UUID)
}

data class PlaceOrderCommand(val accountId: String, val symbol: String, val side: Side, val orderType: OrderType, val quantity: Long, val price: BigDecimal?)
data class CorrectOrderCommand(val newPrice: BigDecimal)

@Service
class OrderApplicationService(
    private val orders: OrderJpaRepository,
    private val corrections: OrderCorrectionJpaRepository,
    private val outbox: OutboxJpaRepository,
    private val processed: ProcessedEventJpaRepository,
    private val mapper: ObjectMapper,
    private val reservationPort: SellReservationPort,
    private val buyReservationPort: BuyReservationPort,
) {
    @Transactional
    fun place(command: PlaceOrderCommand): Order {
        TradingPolicies.validatePriceTick(command.orderType, command.price)
        val order = Order(UUID.randomUUID(), command.accountId.trim(), command.symbol.trim(), command.side,
            command.orderType, command.quantity, command.price)
        var sellReserved = false; var buyReserved = false
        try {
            if (order.side == Side.SELL) { reservationPort.reserve(order.id, order.accountId, order.symbol, order.quantity); sellReserved = true }
            else { buyReservationPort.reserve(order.id, order.accountId, order.symbol, order.orderType, order.quantity, order.price); buyReserved = true }
            orders.save(OrderEntity.from(order))
            emit(Topics.ORDER_PLACED, order.id, OrderPlacedEvent(UUID.randomUUID(), order.id, order.accountId, order.symbol,
                order.side, order.orderType, order.quantity, order.price, Instant.now()))
            return order
        } catch (e: Exception) {
            if (sellReserved) runCatching { reservationPort.release(order.id) }
            if (buyReserved) runCatching { buyReservationPort.release(order.id) }
            throw e
        }
    }

    @Transactional
    fun requestCorrection(orderId: UUID, command: CorrectOrderCommand): OrderCorrectionEntity {
        TradingPolicies.validatePriceTick(OrderType.LIMIT, command.newPrice)
        val entity = orders.findForUpdate(orderId) ?: error("order not found: $orderId")
        val order = entity.toDomain()
        val oldPrice = requireNotNull(order.price) { "MARKET order cannot be corrected" }
        order.requestCorrection(command.newPrice)
        entity.apply(order)
        val correction = corrections.save(OrderCorrectionEntity(
            correctionId = UUID.randomUUID(), orderId = orderId, oldPrice = oldPrice, newPrice = command.newPrice
        ))
        emit(Topics.ORDER_CORRECTION_REQUESTED, order.id,
            OrderCorrectionRequestedEvent(UUID.randomUUID(), correction.correctionId, order.id, command.newPrice, Instant.now()))
        return correction
    }

    @Transactional
    fun requestCancel(orderId: UUID): Order {
        val entity = orders.findForUpdate(orderId) ?: error("order not found: $orderId")
        val order = entity.toDomain(); order.requestCancel(); entity.apply(order)
        emit(Topics.ORDER_CANCEL_REQUESTED, order.id, OrderCancelRequestedEvent(UUID.randomUUID(), order.id, Instant.now()))
        return order
    }

    @Transactional(readOnly = true)
    fun get(orderId: UUID): Order = orders.findById(orderId).orElseThrow { NoSuchElementException("order not found: $orderId") }.toDomain()

    @Transactional(readOnly = true)
    fun corrections(orderId: UUID): List<OrderCorrectionEntity> = corrections.findAllByOrderIdOrderByRequestedAt(orderId)

    @Transactional fun onAccepted(event: OrderAcceptedEvent) = once(event.eventId) { orders.findForUpdate(event.orderId)?.let { val o=it.toDomain(); o.accept(); it.apply(o) } }
    @Transactional fun onRejected(event: OrderRejectedEvent) = once(event.eventId) { orders.findForUpdate(event.orderId)?.let { val o=it.toDomain(); o.reject(); it.apply(o) } }
    @Transactional fun onExecution(event: ExecutionBookedEvent) = once(event.eventId) {
        val entity=orders.findForUpdate(event.orderId) ?: error("order not found: ${event.orderId}"); val o=entity.toDomain(); o.applyExecution(event.quantity); entity.apply(o)
    }
    @Transactional fun onCanceled(event: OrderCanceledEvent) = once(event.eventId) { orders.findForUpdate(event.orderId)?.let { val o=it.toDomain(); o.cancel(); it.apply(o) } }
    @Transactional fun onCancelRejected(event: OrderCancelRejectedEvent) = once(event.eventId) { orders.findForUpdate(event.orderId)?.let { val o=it.toDomain(); o.cancelRejected(); it.apply(o) } }

    @Transactional
    fun onCorrected(event: OrderCorrectedEvent) = once(event.eventId) {
        corrections.findById(event.correctionId).ifPresent { c -> c.status=CorrectionStatus.ACCEPTED; c.resolvedAt=event.occurredAt }
        orders.findForUpdate(event.orderId)?.let { entity ->
            val order=entity.toDomain()
            if (!order.isTerminal) { order.correctionAccepted(event.newPrice); entity.apply(order) }
        }
    }

    @Transactional
    fun onCorrectionRejected(event: OrderCorrectionRejectedEvent) = once(event.eventId) {
        corrections.findById(event.correctionId).ifPresent { c -> c.status=CorrectionStatus.REJECTED; c.resolvedAt=event.occurredAt }
        orders.findForUpdate(event.orderId)?.let { entity -> val order=entity.toDomain(); order.correctionRejected(); entity.apply(order) }
    }

    private fun emit(topic: String, orderId: UUID, event: Any) = outbox.save(OutboxEntity(topic=topic, eventKey=orderId.toString(), payload=mapper.writeValueAsString(event)))
    private fun once(eventId: UUID, block: () -> Unit) { if (processed.existsById(eventId)) return; block(); processed.save(ProcessedEventEntity(eventId)) }
}
