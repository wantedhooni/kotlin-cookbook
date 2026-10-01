package com.example.revy.service

import com.example.revy.domain.OrderRepository
import com.example.revy.domain.OrderSummary
import com.example.revy.domain.UserRepository
import kotlinx.coroutines.flow.first
import org.springframework.stereotype.Service

@Service
class OrderSummaryService(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val enrichmentService: EnrichmentService,
) {

    suspend fun getOrderSummary(userId: String): OrderSummary {
        val user = requireNotNull(userRepository.findById(userId)) { "User not found: $userId" }
        val order = orderRepository.findByUserId(user.id).first()
        val enriched = enrichmentService.enrich(order)
        return OrderSummary(user, enriched)
    }
}
