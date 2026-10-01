package com.example.revy.domain


import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("users")
data class User(
    @Id
    @Column("id")
    val id: String,
    @Column("name")
    val name: String,
    @Column("email")
    val email: String,
)

@Table("orders")
data class Order(
    @Id
    @Column("id")
    val id: String,
    @Column("user_id")
    val userId: String,
    @Column("item")
    val item: String,
    @Column("amount")
    val amount: BigDecimal,
)

/** Result of calling out to an (imaginary) enrichment service for a single order. */
data class EnrichedOrder(
    val order: Order,
    val estimatedDeliveryDays: Int,
)

data class OrderSummary(
    val user: User,
    val enrichedOrder: EnrichedOrder,
)

data class Profile(
    val id: String,
    val displayName: String,
)

data class Notification(
    val id: String,
    val message: String,
)

data class UserDashboard(
    val profile: Profile,
    val orders: List<Order>,
    val notifications: List<Notification>,
)