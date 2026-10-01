package com.example.trading.executionservice.adapter.`in`.kafka

import com.example.trading.event.KrxExecutionEvent
import com.example.trading.event.Topics
import com.example.trading.executionservice.application.ExecutionApplicationService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class KrxExecutionListener(private val mapper: ObjectMapper, private val service: ExecutionApplicationService) {
    @KafkaListener(topics = [Topics.KRX_EXECUTION], groupId = "execution-service")
    fun consume(payload: String) = service.book(mapper.readValue(payload, KrxExecutionEvent::class.java))
}
