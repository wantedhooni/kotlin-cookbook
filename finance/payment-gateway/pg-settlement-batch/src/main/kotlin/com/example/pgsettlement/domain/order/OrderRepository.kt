package com.example.pgsettlement.domain.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<OrderEntity, Long> {
    fun findByOrderNo(orderNo: String): OrderEntity?
}
