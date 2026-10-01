package com.example.revy.domain.account

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "ledger_entry",
    indexes = [
        Index(
            name = "idx_ledger_account_occurred",
            columnList = "account_id, occurred_at"
        ),
        Index(
            name = "idx_ledger_reference",
            columnList = "reference_type, reference_id"
        )
    ]
)
class LedgerEntry(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "account_id",
        nullable = false
    )
    val account: Account,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val entryType: LedgerEntryType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: LedgerDirection,

    @Column(
        nullable = false,
        precision = 24,
        scale = 8
    )
    val amount: BigDecimal,

    @Column(
        nullable = false,
        length = 3
    )
    val currency: String,

    @Column(
        name = "balance_after",
        nullable = false,
        precision = 24,
        scale = 8
    )
    val balanceAfter: BigDecimal,

    @Column(
        name = "reference_type",
        length = 30
    )
    val referenceType: String? = null,

    @Column(
        name = "reference_id",
        length = 100
    )
    val referenceId: String? = null,

    @Column(nullable = false)
    val occurredAt: Instant,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class LedgerEntryType {
    DEPOSIT,
    WITHDRAWAL,

    TRANSFER_IN,
    TRANSFER_OUT,

    FX_BUY,
    FX_SELL,

    STOCK_BUY,
    STOCK_SELL,

    FEE,
    INTEREST,
    DIVIDEND,

    ADJUSTMENT
}

enum class LedgerDirection {
    CREDIT,
    DEBIT
}