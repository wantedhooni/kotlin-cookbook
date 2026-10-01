package com.example.revy.service

import com.example.revy.domain.EnrichedOrder
import com.example.revy.domain.Order
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.milliseconds


@Service
class EnrichmentService {

    suspend fun enrich(order: Order): EnrichedOrder {
        delay(10.milliseconds)
        val days = 2 + (order.item.length % 5)
        return EnrichedOrder(order = order, estimatedDeliveryDays = days)
    }
}
