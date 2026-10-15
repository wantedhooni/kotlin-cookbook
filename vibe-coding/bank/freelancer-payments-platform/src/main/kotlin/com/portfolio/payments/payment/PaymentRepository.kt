package com.portfolio.payments.payment

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, String> {
    fun findByIdempotencyKey(idempotencyKey: String): Payment?
    fun findAllByStatus(status: PaymentStatus): List<Payment>
}
