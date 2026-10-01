package com.example.revy.service

import com.example.revy.domain.Notification
import com.example.revy.domain.Order
import com.example.revy.domain.OrderRepository
import com.example.revy.domain.Profile
import com.example.revy.domain.UserDashboard
import com.example.revy.domain.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class ProfileService(private val userRepository: UserRepository) {

    suspend fun getProfile(userId: String): Profile {
        val user = requireNotNull(userRepository.findById(userId)) { "User not found: $userId" }
        return Profile(id = user.id, displayName = user.name)
    }
}

@Service
class OrderQueryService(private val orderRepository: OrderRepository) {

    suspend fun getRecentOrders(userId: String): List<Order> =
        orderRepository.findByUserId(userId).toList()
}

@Service
class NotificationService {

    suspend fun getUnread(userId: String): List<Notification> {
        delay(10)
        return listOf(Notification(id = "notif-1", message = "Your order shipped!"))
    }
}

/**
 * Need to call three services at once and combine the results. Every result
 * stays in a named variable, all the way through - no positional Tuple3.
 */
@Service
class DashboardService(
    private val profileService: ProfileService,
    private val orderQueryService: OrderQueryService,
    private val notificationService: NotificationService,
) {

    suspend fun getDashboard(userId: String): UserDashboard = coroutineScope {
        val profile = async { profileService.getProfile(userId) }
        val orders = async { orderQueryService.getRecentOrders(userId) }
        val notifications = async { notificationService.getUnread(userId) }

        UserDashboard(profile.await(), orders.await(), notifications.await())
    }
}
