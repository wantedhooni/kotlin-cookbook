package com.portfolio.payments.payment

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [UniqueConstraint(name = "uk_payment_idempotency", columnNames = ["idempotency_key"])]
)
class Payment(
    @Id
    val id: String,
    @Column(name = "seller_id", nullable = false)
    val sellerId: String,
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    val idempotencyKey: String,
    @Column(nullable = false)
    val amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = PaymentStatus.REQUESTED,
    @Column(name = "van_transaction_id")
    var vanTransactionId: String? = null,
    @Column(name = "failure_reason")
    var failureReason: String? = null,
    @Version
    var version: Long? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "approved_at")
    var approvedAt: Instant? = null,
) {
    fun approve(vanTxId: String) {
        require(status == PaymentStatus.REQUESTED) { "only requested payment can be approved" }
        status = PaymentStatus.APPROVED
        vanTransactionId = vanTxId
        approvedAt = Instant.now()
    }

    fun cancel(reason: String) {
        status = PaymentStatus.CANCELED
        failureReason = reason
    }

    fun fail(reason: String) {
        status = PaymentStatus.FAILED
        failureReason = reason
    }
}

enum class PaymentStatus { REQUESTED, APPROVED, CANCELED, FAILED }
