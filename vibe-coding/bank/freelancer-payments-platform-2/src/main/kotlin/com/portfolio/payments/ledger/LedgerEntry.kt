package com.portfolio.payments.ledger

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "ledger_entries")
class LedgerEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "account_key", nullable = false)
    val accountKey: String,
    @Column(name = "reference_id", nullable = false)
    val referenceId: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val direction: Direction,
    @Column(nullable = false)
    val amount: Long,
    @Column(nullable = false)
    val type: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

enum class Direction { DEBIT, CREDIT }

interface LedgerEntryRepository : org.springframework.data.jpa.repository.JpaRepository<LedgerEntry, Long> {
    fun findAllByReferenceId(referenceId: String): List<LedgerEntry>
}
