package com.example.revy.controller

import com.example.revy.domain.OrderSummary
import com.example.revy.domain.User
import com.example.revy.domain.UserDashboard
import com.example.revy.domain.UserRepository
import com.example.revy.service.DashboardService
import com.example.revy.service.LegacyUserService
import com.example.revy.service.OrderSummaryService
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class UserController(
    private val userRepository: UserRepository,
    private val legacyUserService: LegacyUserService,
    private val orderSummaryService: OrderSummaryService,
    private val dashboardService: DashboardService,
) {

    /** New coroutine endpoint calling still-unconverted Reactor code. */
    @GetMapping("/users/{id}")
    suspend fun getUser(@PathVariable id: String): User =
        legacyUserService.findById(id).awaitSingle()

    /** New coroutine code exposed as Mono<User> for callers that haven't migrated yet. */
    @GetMapping("/users/{id}/reactive")
    fun getUserReactive(@PathVariable id: String): Mono<User> = mono {
        requireNotNull(userRepository.findById(id)) { "User not found: $id" }
    }

    @GetMapping("/users/{id}/order-summary")
    suspend fun getOrderSummary(@PathVariable id: String): OrderSummary =
        orderSummaryService.getOrderSummary(id)

    @GetMapping("/users/{id}/dashboard")
    suspend fun getDashboard(@PathVariable id: String): UserDashboard =
        dashboardService.getDashboard(id)
}
