package com.portfolio.payments.settlement

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "settlements", uniqueConstraints = [UniqueConstraint(name="uk_settlement_payment", columnNames=["payment_id"])])
class Settlement(
    @Id val id: String,
    @Column(name = "payment_id", nullable = false) val paymentId: String,
    @Column(name = "seller_id", nullable = false) val sellerId: String,
    @Column(name = "gross_amount", nullable = false) val grossAmount: Long,
    @Column(name = "platform_fee", nullable = false) val platformFee: Long,
    @Column(name = "withholding_tax", nullable = false) val withholdingTax: Long,
    @Column(name = "payout_amount", nullable = false) val payoutAmount: Long,
    @Enumerated(EnumType.STRING) @Column(nullable = false) var status: SettlementStatus = SettlementStatus.READY,
    @Column(name = "bank_transfer_id") var bankTransferId: String? = null,
    @Version var version: Long? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
) {
    fun paid(transferId: String) { status = SettlementStatus.PAID; bankTransferId = transferId }
    fun payoutFailed() { status = SettlementStatus.PAYOUT_FAILED }
}

enum class SettlementStatus { READY, PAID, PAYOUT_FAILED }

interface SettlementRepository : org.springframework.data.jpa.repository.JpaRepository<Settlement, String> {
    fun findByPaymentId(paymentId: String): Settlement?
}
