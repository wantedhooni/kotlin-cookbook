package com.example.trs.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class PayoutStatus { PENDING, SUCCEEDED, FAILED }

@Entity
@Table(name = "payouts", uniqueConstraints = [UniqueConstraint(name = "uk_payout_idempotency", columnNames = ["idempotency_key"])])
class Payout(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_id", nullable = false)
    var refund: Refund,
    @Column(name = "idempotency_key", nullable = false)
    var idempotencyKey: String,
    @Column(nullable = false, precision = 18, scale = 0)
    var amount: BigDecimal,
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var status: PayoutStatus = PayoutStatus.PENDING,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)
