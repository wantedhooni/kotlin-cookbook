package com.example.trs.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "purchases", uniqueConstraints = [UniqueConstraint(name = "uk_purchase_external", columnNames = ["merchant_id", "external_tx_id"])])
class Purchase(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    var merchant: Merchant,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tourist_id", nullable = false)
    var tourist: Tourist,
    @Column(name = "external_tx_id", nullable = false)
    var externalTransactionId: String,
    @Column(nullable = false, precision = 18, scale = 0)
    var totalAmount: BigDecimal,
    @Column(nullable = false)
    var purchasedAt: LocalDateTime,
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var status: PurchaseStatus = PurchaseStatus.COMPLETED,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
) {
    fun requireRefundable() {
        check(status == PurchaseStatus.COMPLETED) { "취소된 구매는 환급할 수 없습니다." }
    }
}
