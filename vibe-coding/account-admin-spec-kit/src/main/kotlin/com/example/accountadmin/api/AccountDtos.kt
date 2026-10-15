package com.example.accountadmin.api

import com.example.accountadmin.domain.Account
import com.example.accountadmin.domain.AccountRole
import com.example.accountadmin.domain.AccountStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.io.Serializable
import java.util.UUID

data class CreateAccountRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "3~50 characters: letters, numbers, dot, underscore or hyphen")
    val loginId: String,
    @field:NotBlank @field:Email @field:Size(max = 255)
    val email: String,
    @field:NotBlank @field:Size(max = 100)
    val displayName: String,
    val role: AccountRole = AccountRole.USER
)

data class UpdateAccountRequest(
    @field:NotBlank @field:Email @field:Size(max = 255)
    val email: String,
    @field:NotBlank @field:Size(max = 100)
    val displayName: String
)

data class ChangeRoleRequest(val role: AccountRole)
data class ChangeStatusRequest(val status: AccountStatus)

data class AccountResponse(
    val id: UUID,
    val loginId: String,
    val email: String,
    val displayName: String,
    val role: AccountRole,
    val status: AccountStatus,
    val createdAt: Instant,
    val updatedAt: Instant
) : Serializable

data class AccountPageResponse(
    val items: List<AccountResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

fun Account.toResponse() = AccountResponse(
    id = id, loginId = loginId, email = email, displayName = displayName,
    role = role, status = status, createdAt = createdAt, updatedAt = updatedAt
)
