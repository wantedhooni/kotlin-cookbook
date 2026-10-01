package com.example.trading.executionservice.application

import com.example.trading.event.*
import com.example.trading.executionservice.adapter.out.persistence.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExecutionApplicationService(
    private val executions: ExecutionJpaRepository,
    private val outbox: ExecutionOutboxJpaRepository,
    private val mapper: ObjectMapper,
) {
    @Transactional
    fun book(event: KrxExecutionEvent) {
        if (executions.existsByExecutionId(event.executionId)) return
        require(event.executionQuantity > 0) { "execution quantity must be positive" }
        executions.save(
            ExecutionEntity(
                executionId = event.executionId, orderId = event.orderId, accountId = event.accountId,
                symbol = event.symbol, side = event.side, quantity = event.executionQuantity,
                price = event.executionPrice, occurredAt = event.occurredAt
            )
        )
        val booked = ExecutionBookedEvent(
            UUID.randomUUID(), event.executionId, event.orderId, event.accountId,
            event.symbol, event.side, event.executionQuantity, event.executionPrice, event.occurredAt
        )
        outbox.save(ExecutionOutboxEntity(topic = Topics.EXECUTION_BOOKED, eventKey = event.orderId.toString(), payload = mapper.writeValueAsString(booked)))
    }
}
