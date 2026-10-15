package com.example.trs.domain

import jakarta.persistence.*

@Entity
@Table(name = "merchants", uniqueConstraints = [UniqueConstraint(name = "uk_merchant_business_number", columnNames = ["business_number"])])
class Merchant(
    @Column(nullable = false) var name: String,
    @Column(name = "business_number", nullable = false) var businessNumber: String,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)
