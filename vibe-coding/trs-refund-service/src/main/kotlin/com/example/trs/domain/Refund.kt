package com.example.trs.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "refunds", uniqueConstraints = [UniqueConstraint(name = "uk_refund_document", columnNames = ["document_id"])])
class Refund(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    var document: RefundDocument,
    @Column(nullable = false, precision = 18, scale = 0)
    var amount: BigDecimal,
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var status: RefundStatus = RefundStatus.REQUESTED,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Version var version: Long? = null,
) {
    init { require(amount > BigDecimal.ZERO) { "환급액은 양수여야 합니다." } }

    fun approve() = transition(RefundStatus.REQUESTED, RefundStatus.APPROVED)
    fun startPayout() = transition(RefundStatus.APPROVED, RefundStatus.PROCESSING)
    fun complete() = transition(RefundStatus.PROCESSING, RefundStatus.COMPLETED)
    fun fail() = transition(RefundStatus.PROCESSING, RefundStatus.FAILED)
    fun cancel() = transition(RefundStatus.REQUESTED, RefundStatus.CANCELLED)

    private fun transition(expected: RefundStatus, next: RefundStatus) {
        check(status == expected) { "잘못된 상태 전이: $status -> $next" }
        status = next
    }
}
