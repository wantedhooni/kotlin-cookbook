package com.example.trading.executionservice.adapter.`in`.web

import com.example.trading.executionservice.adapter.out.persistence.ExecutionJpaRepository
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/executions")
class ExecutionQueryController(private val repository: ExecutionJpaRepository) {
    @GetMapping("/orders/{orderId}")
    fun byOrder(@PathVariable orderId: UUID) = repository.findAllByOrderIdOrderByOccurredAt(orderId).map {
        mapOf("executionId" to it.executionId, "orderId" to it.orderId, "symbol" to it.symbol,
            "side" to it.side, "quantity" to it.quantity, "price" to it.price, "occurredAt" to it.occurredAt)
    }
}
