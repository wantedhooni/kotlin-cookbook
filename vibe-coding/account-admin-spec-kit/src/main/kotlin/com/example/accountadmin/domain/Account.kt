package com.example.accountadmin.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "accounts")
class Account(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    var loginId: String = "",

    @Column(nullable = false, unique = true, length = 255)
    var email: String = "",

    @Column(name = "display_name", nullable = false, length = 100)
    var displayName: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: AccountRole = AccountRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AccountStatus = AccountStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun touch() { updatedAt = Instant.now() }
}

enum class AccountRole { USER, ADMIN }
enum class AccountStatus { ACTIVE, SUSPENDED, DELETED }
