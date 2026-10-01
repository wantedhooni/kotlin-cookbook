package com.example.revy.domain.account

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "external_account"
)
class ExternalAccount(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val customerId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "institution_id",
        nullable = false
    )
    val institution: FinancialInstitution,

    @Column(
        nullable = false,
        length = 50
    )
    val accountNumber: String,

    @Column(
        nullable = false,
        length = 100
    )
    val accountHolderName: String,

    @Column(nullable = false)
    var verified: Boolean = false,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)