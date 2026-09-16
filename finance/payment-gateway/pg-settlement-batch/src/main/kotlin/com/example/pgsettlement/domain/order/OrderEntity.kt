package com.example.pgsettlement.domain.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "orders")
class OrderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    var orderNo: String,

    @Column(name = "order_amount", nullable = false, precision = 19, scale = 2)
    var orderAmount: BigDecimal,
)
