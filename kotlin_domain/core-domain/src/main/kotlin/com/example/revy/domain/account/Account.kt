package com.example.revy.domain.account

import com.example.revy.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Version

class Account : BaseEntity() {

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set
}