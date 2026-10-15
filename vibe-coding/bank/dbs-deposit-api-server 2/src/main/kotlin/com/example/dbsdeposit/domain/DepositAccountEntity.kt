package com.example.dbsdeposit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "deposit_account",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_deposit_account_party_external",
            columnNames = ["party_id", "external_account_id"],
        ),
    ],
)
class DepositAccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "party_id", nullable = false, length = 128)
    var partyId: String,

    @Column(name = "external_account_id", nullable = false, length = 128)
    var externalAccountId: String,

    @Column(name = "account_number_masked", length = 128)
    var accountNumberMasked: String? = null,

    @Column(name = "product_name", length = 255)
    var productName: String? = null,

    @Column(name = "product_type", length = 128)
    var productType: String? = null,

    @Column(name = "currency", length = 3)
    var currency: String? = null,

    @Column(name = "available_balance", precision = 19, scale = 4)
    var availableBalance: BigDecimal? = null,

    @Lob
    @Column(name = "raw_json", nullable = false)
    var rawJson: String,

    @Column(name = "provider_updated_at")
    var providerUpdatedAt: Instant? = null,

    @Column(name = "synced_at", nullable = false)
    var syncedAt: Instant = Instant.now(),
)
