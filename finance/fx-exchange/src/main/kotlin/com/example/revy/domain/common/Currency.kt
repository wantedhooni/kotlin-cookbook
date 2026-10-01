package com.example.revy.domain.common

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "currency")
class Currency(

    @Id
    @Column(length = 3)
    val code: String, // USD, KRW, JPY

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val decimalPlaces: Int,

    @Column(nullable = false)
    val enabled: Boolean = true
)