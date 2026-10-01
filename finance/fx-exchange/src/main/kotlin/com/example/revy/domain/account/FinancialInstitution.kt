package com.example.revy.domain.account

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "financial_institution",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_financial_institution_code",
            columnNames = ["institution_code"]
        )
    ]
)
class FinancialInstitution(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(
        name = "institution_code",
        nullable = false,
        length = 20
    )
    val institutionCode: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: FinancialInstitutionType,

    @Column(nullable = false)
    val enabled: Boolean = true
)

enum class FinancialInstitutionType {
    BANK,
    SECURITIES
}