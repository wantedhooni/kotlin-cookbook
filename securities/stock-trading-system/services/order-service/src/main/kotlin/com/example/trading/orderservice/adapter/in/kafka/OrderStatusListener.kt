package com.example.trading.orderservice.adapter.`in`.kafka

import com.example.trading.event.*
import com.example.trading.orderservice.application.OrderApplicationService
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class OrderStatusListener(private val service: OrderApplicationService, private val mapper: ObjectMapper) {
    @KafkaListener(topics=[Topics.ORDER_ACCEPTED,Topics.ORDER_REJECTED,Topics.EXECUTION_BOOKED,Topics.ORDER_CANCELED,
        Topics.ORDER_CANCEL_REJECTED,Topics.ORDER_CORRECTED,Topics.ORDER_CORRECTION_REJECTED], groupId="order-service-status")
    fun consume(record:ConsumerRecord<String,String>) { when(record.topic()) {
        Topics.ORDER_ACCEPTED -> service.onAccepted(mapper.readValue(record.value(),OrderAcceptedEvent::class.java))
        Topics.ORDER_REJECTED -> service.onRejected(mapper.readValue(record.value(),OrderRejectedEvent::class.java))
        Topics.EXECUTION_BOOKED -> service.onExecution(mapper.readValue(record.value(),ExecutionBookedEvent::class.java))
        Topics.ORDER_CANCELED -> service.onCanceled(mapper.readValue(record.value(),OrderCanceledEvent::class.java))
        Topics.ORDER_CANCEL_REJECTED -> service.onCancelRejected(mapper.readValue(record.value(),OrderCancelRejectedEvent::class.java))
        Topics.ORDER_CORRECTED -> service.onCorrected(mapper.readValue(record.value(),OrderCorrectedEvent::class.java))
        Topics.ORDER_CORRECTION_REJECTED -> service.onCorrectionRejected(mapper.readValue(record.value(),OrderCorrectionRejectedEvent::class.java))
    }}
}
