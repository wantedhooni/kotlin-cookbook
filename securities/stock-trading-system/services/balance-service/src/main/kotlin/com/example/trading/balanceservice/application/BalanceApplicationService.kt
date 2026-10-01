package com.example.trading.balanceservice.application

import com.example.trading.balanceservice.adapter.out.persistence.*
import com.example.trading.domain.Position
import com.example.trading.domain.Side
import com.example.trading.event.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class BalanceApplicationService(
    private val positions: PositionJpaRepository,
    private val reservations: SellReservationJpaRepository,
    private val processed: BalanceProcessedEventJpaRepository,
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
) {
    @Transactional
    fun reserveSell(orderId: UUID, accountId: String, symbol: String, quantity: Long) {
        require(quantity > 0)
        if (reservations.existsById(orderId)) return
        val position = positions.findForUpdate(accountId, symbol) ?: error("position not found for sell order")
        val available = position.quantity - position.reservedQuantity
        check(available >= quantity) { "insufficient available quantity: available=$available requested=$quantity" }
        position.reservedQuantity += quantity
        position.updatedAt = Instant.now()
        reservations.save(SellReservationEntity(orderId, accountId, symbol, quantity))
    }

    @Transactional
    fun releaseSell(orderId: UUID) {
        val r = reservations.findForUpdate(orderId) ?: return
        if (r.status != ReservationStatus.ACTIVE) return
        val remaining = r.quantity - r.consumedQuantity - r.canceledQuantity
        positions.findForUpdate(r.accountId, r.symbol)?.let { p ->
            p.reservedQuantity = (p.reservedQuantity - remaining).coerceAtLeast(0)
            p.updatedAt = Instant.now()
            cacheBestEffort(p)
        }
        r.canceledQuantity += remaining
        r.status = ReservationStatus.RELEASED
    }

    @Transactional
    fun applyExecution(event: ExecutionBookedEvent) = once(event.eventId) {
        val entity = positions.findForUpdate(event.accountId, event.symbol)
            ?: if (event.side == Side.BUY) PositionEntity(accountId = event.accountId, symbol = event.symbol) else error("position not found for sell execution")
        val domain = Position(entity.accountId, entity.symbol, entity.quantity, entity.averagePrice, entity.realizedPnl)
        if (event.side == Side.BUY) {
            domain.buy(event.quantity, event.price)
        } else {
            val reservation = reservations.findForUpdate(event.orderId) ?: error("sell reservation missing for order ${event.orderId}")
            check(reservation.status == ReservationStatus.ACTIVE) { "sell reservation is not active" }
            val remainingReserved = reservation.quantity - reservation.consumedQuantity - reservation.canceledQuantity
            check(remainingReserved >= event.quantity) { "execution exceeds reserved quantity" }
            domain.sell(event.quantity, event.price)
            reservation.consumedQuantity += event.quantity
            entity.reservedQuantity = (entity.reservedQuantity - event.quantity).coerceAtLeast(0)
            if (reservation.consumedQuantity + reservation.canceledQuantity == reservation.quantity) {
                reservation.status = if (reservation.canceledQuantity > 0) ReservationStatus.RELEASED else ReservationStatus.CONSUMED
            }
        }
        entity.quantity = domain.quantity
        entity.averagePrice = domain.averagePrice
        entity.realizedPnl = domain.realizedPnl
        entity.updatedAt = Instant.now()
        positions.save(entity)
        cacheBestEffort(entity)
    }

    @Transactional
    fun onCanceled(event: OrderCanceledEvent) = once(event.eventId) {
        val r = reservations.findForUpdate(event.orderId) ?: return@once
        if (r.status != ReservationStatus.ACTIVE) return@once
        val unaccounted = r.quantity - r.consumedQuantity - r.canceledQuantity
        check(event.canceledQuantity in 0..unaccounted) { "invalid canceled quantity" }
        positions.findForUpdate(r.accountId, r.symbol)?.let { p ->
            p.reservedQuantity = (p.reservedQuantity - event.canceledQuantity).coerceAtLeast(0)
            p.updatedAt = Instant.now()
            cacheBestEffort(p)
        }
        r.canceledQuantity += event.canceledQuantity
        if (r.consumedQuantity + r.canceledQuantity == r.quantity) r.status = ReservationStatus.RELEASED
    }

    @Transactional
    fun onRejected(event: OrderRejectedEvent) = once(event.eventId) { releaseSell(event.orderId) }

    @Transactional
    fun seed(accountId: String, symbol: String, quantity: Long, averagePrice: BigDecimal): PositionView {
        require(quantity >= 0 && averagePrice >= BigDecimal.ZERO)
        val entity = positions.findForUpdate(accountId, symbol) ?: PositionEntity(accountId = accountId, symbol = symbol)
        check(entity.reservedQuantity == 0L) { "cannot seed a position with active reservations" }
        entity.quantity = quantity
        entity.averagePrice = if (quantity == 0L) BigDecimal.ZERO else averagePrice
        entity.updatedAt = Instant.now()
        positions.save(entity)
        cacheBestEffort(entity)
        return entity.toView()
    }

    @Transactional(readOnly = true)
    fun get(accountId: String, symbol: String): PositionView {
        val key = cacheKey(accountId, symbol)
        runCatching { redis.opsForValue().get(key) }.getOrNull()?.let { return mapper.readValue(it, PositionView::class.java) }
        val entity = positions.findByAccountIdAndSymbol(accountId, symbol) ?: PositionEntity(accountId = accountId, symbol = symbol)
        cacheBestEffort(entity)
        return entity.toView()
    }

    @Transactional(readOnly = true)
    fun list(accountId: String): List<PositionView> = positions.findAllByAccountIdOrderBySymbol(accountId).map { it.toView() }

    private fun once(eventId: UUID, block: () -> Unit) {
        if (processed.existsById(eventId)) return
        block()
        processed.save(BalanceProcessedEventEntity(eventId))
    }

    private fun cacheBestEffort(entity: PositionEntity) {
        runCatching {
            redis.opsForValue().set(cacheKey(entity.accountId, entity.symbol), mapper.writeValueAsString(entity.toView()), Duration.ofSeconds(30))
        }
    }

    private fun cacheKey(accountId: String, symbol: String) = "position:$accountId:$symbol"
}

data class PositionView(
    val accountId: String,
    val symbol: String,
    val quantity: Long,
    val reservedQuantity: Long,
    val availableQuantity: Long,
    val averagePrice: BigDecimal,
    val realizedPnl: BigDecimal,
    val updatedAt: Instant,
)

fun PositionEntity.toView() = PositionView(accountId, symbol, quantity, reservedQuantity, quantity - reservedQuantity, averagePrice, realizedPnl, updatedAt)
