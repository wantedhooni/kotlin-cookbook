package com.example.revy.domain.account

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "account_holder",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_holder",
            columnNames = [
                "account_id",
                "customer_id"
            ]
        )
    ]
)
class AccountHolder(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "account_id",
        nullable = false
    )
    val account: Account,

    @Column(
        name = "customer_id",
        nullable = false
    )
    val customerId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val holderType: AccountHolderType,

    @Column(nullable = false)
    val primaryHolder: Boolean = false
)

enum class AccountHolderType {
    OWNER,
    JOINT_OWNER,
    BENEFICIARY,
    AUTHORIZED_USER
}