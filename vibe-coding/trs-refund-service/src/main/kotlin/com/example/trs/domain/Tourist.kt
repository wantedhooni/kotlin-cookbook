package com.example.trs.domain

import jakarta.persistence.*

@Entity
@Table(name = "tourists", uniqueConstraints = [UniqueConstraint(name = "uk_tourist_token", columnNames = ["passport_token"])])
class Tourist(
    @Column(name = "passport_token", nullable = false) var passportToken: String,
    @Column(nullable = false, length = 2) var nationality: String,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)
