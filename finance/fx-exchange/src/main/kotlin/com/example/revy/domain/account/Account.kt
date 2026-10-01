package com.example.revy.domain.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "account",
    indexes = [
        Index(
            name = "idx_account_customer_id",
            columnList = "customer_id"
        ),
        Index(
            name = "idx_account_institution",
            columnList = "institution_id"
        )
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_institution_number",
            columnNames = [
                "institution_id",
                "account_number"
            ]
        )
    ]
)
class Account(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(
        name = "customer_id",
        nullable = false
    )
    val customerId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "institution_id",
        nullable = false
    )
    val institution: FinancialInstitution,

    @Column(
        name = "account_number",
        nullable = false,
        length = 50
    )
    val accountNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(
        name = "account_type",
        nullable = false,
        length = 30
    )
    val accountType: AccountType,

    @Column(
        name = "account_name",
        nullable = false,
        length = 100
    )
    var accountName: String,

    @Column(
        name = "currency",
        nullable = false,
        length = 3
    )
    val currency: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AccountStatus = AccountStatus.ACTIVE,

    @Column(nullable = false)
    val openedAt: LocalDate,

    var closedAt: LocalDate? = null,

    @Version
    var version: Long = 0
)

enum class AccountType {
    CHECKING,
    SAVINGS,
    CMA,
    BROKERAGE,
    ISA,
    PENSION,
    FOREIGN_CURRENCY
}

enum class AccountStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED
}