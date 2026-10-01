package com.example.trading.orderrouter.adapter.`in`.kafka

import com.example.trading.domain.*
import com.example.trading.event.*
import com.example.trading.orderrouter.domain.MarketSessionResolver
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class OrderRoutingListener(
    private val mapper: ObjectMapper,
    private val kafka: KafkaTemplate<String, String>,
    private val sessions: MarketSessionResolver,
) {
    @KafkaListener(topics = [Topics.ORDER_PLACED], groupId = "order-router-place")
    fun placed(payload: String) {
        val order = mapper.readValue(payload, OrderPlacedEvent::class.java)
        val session = sessions.current()
        if (!TradingPolicies.isOrderTypeAllowed(session, order.orderType)) {
            val rejected = OrderRejectedEvent(UUID.randomUUID(), order.orderId, "SESSION_RULE", "${order.orderType} is not allowed in $session", Instant.now())
            send(Topics.ORDER_REJECTED, order.orderId.toString(), rejected)
            return
        }
        send(Topics.ORDER_ACCEPTED, order.orderId.toString(), OrderAcceptedEvent(UUID.randomUUID(), order.orderId, session, Instant.now()))
        send(
            Topics.KRX_ORDER_REQUEST, order.orderId.toString(),
            KrxOrderRequest(UUID.randomUUID(), order.orderId, order.accountId, order.symbol, order.side, order.orderType, order.quantity, order.price, session, Instant.now())
        )
    }

    @KafkaListener(topics = [Topics.ORDER_CORRECTION_REQUESTED], groupId = "order-router-correction")
    fun correction(payload: String) {
        val event = mapper.readValue(payload, OrderCorrectionRequestedEvent::class.java)
        val session = sessions.current()
        if (!TradingPolicies.isOrderTypeAllowed(session, OrderType.LIMIT)) {
            send(Topics.ORDER_CORRECTION_REJECTED, event.orderId.toString(),
                OrderCorrectionRejectedEvent(UUID.randomUUID(), event.correctionId, event.orderId, "correction is not allowed in $session", Instant.now()))
            return
        }
        send(Topics.KRX_CORRECTION_REQUEST, event.orderId.toString(),
            KrxCorrectionRequest(UUID.randomUUID(), event.correctionId, event.orderId, event.newPrice, Instant.now()))
    }

    @KafkaListener(topics = [Topics.ORDER_CANCEL_REQUESTED], groupId = "order-router-cancel")
    fun cancel(payload: String) {
        val event = mapper.readValue(payload, OrderCancelRequestedEvent::class.java)
        send(Topics.KRX_CANCEL_REQUEST, event.orderId.toString(), KrxCancelRequest(UUID.randomUUID(), event.orderId, Instant.now()))
    }

    private fun send(topic: String, key: String, value: Any) {
        kafka.send(topic, key, mapper.writeValueAsString(value)).get(5, TimeUnit.SECONDS)
    }
}
