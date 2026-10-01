package com.example.trading.balanceservice.adapter.`in`.kafka

import com.example.trading.balanceservice.application.BalanceApplicationService
import com.example.trading.event.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class BalanceEventListener(private val mapper: ObjectMapper, private val service: BalanceApplicationService) {
    @KafkaListener(topics = [Topics.EXECUTION_BOOKED, Topics.ORDER_CANCELED, Topics.ORDER_REJECTED], groupId = "balance-service")
    fun consume(record: ConsumerRecord<String, String>) {
        when (record.topic()) {
            Topics.EXECUTION_BOOKED -> service.applyExecution(mapper.readValue(record.value(), ExecutionBookedEvent::class.java))
            Topics.ORDER_CANCELED -> service.onCanceled(mapper.readValue(record.value(), OrderCanceledEvent::class.java))
            Topics.ORDER_REJECTED -> service.onRejected(mapper.readValue(record.value(), OrderRejectedEvent::class.java))
        }
    }
}
