package com.example.accountadmin.service

import com.example.accountadmin.api.*
import com.example.accountadmin.domain.Account
import com.example.accountadmin.domain.AccountRole
import com.example.accountadmin.domain.AccountStatus
import com.example.accountadmin.repository.AccountRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import java.util.UUID

@Service
class AccountService(
    private val repository: AccountRepository,
    private val events: ApplicationEventPublisher
) {
    @Transactional(readOnly = true)
    fun list(keyword: String?, role: AccountRole?, status: AccountStatus?, page: Int, size: Int): AccountPageResponse {
        val result = repository.search(keyword, role, status, PageRequest.of(page, size))
        return AccountPageResponse(
            items = result.content.map { it.toResponse() }, page = result.number,
            size = result.size, totalElements = result.totalElements, totalPages = result.totalPages
        )
    }

    @Cacheable(cacheNames = ["accounts"], key = "#p0.toString()")
    @Transactional(readOnly = true)
    fun get(id: UUID): AccountResponse = findActive(id).toResponse()

    @Transactional
    fun create(request: CreateAccountRequest): AccountResponse {
        val loginId = request.loginId.trim().lowercase(Locale.ROOT)
        val email = normalizeEmail(request.email)
        if (repository.existsByLoginId(loginId) || repository.existsByEmail(email)) {
            throw DuplicateAccountException("Account already exists")
        }
        val saved = repository.save(Account(
            loginId = loginId, email = email, displayName = request.displayName.trim(), role = request.role
        ))
        emit(saved.id, "ACCOUNT_CREATED")
        return saved.toResponse()
    }

    @Transactional
    @CacheEvict(cacheNames = ["accounts"], key = "#p0.toString()")
    fun update(id: UUID, request: UpdateAccountRequest): AccountResponse {
        val account = findActive(id)
        val nextEmail = normalizeEmail(request.email)
        if (nextEmail != account.email && repository.existsByEmail(nextEmail)) {
            throw DuplicateAccountException("Email already exists")
        }
        val nextDisplayName = request.displayName.trim()
        if (nextEmail != account.email || nextDisplayName != account.displayName) {
            account.email = nextEmail
            account.displayName = nextDisplayName
            account.touch()
            emit(id, "ACCOUNT_UPDATED")
        }
        return account.toResponse()
    }

    @Transactional
    @CacheEvict(cacheNames = ["accounts"], key = "#p0.toString()")
    fun changeRole(id: UUID, request: ChangeRoleRequest): AccountResponse {
        val account = findActive(id)
        if (account.role != request.role) {
            account.role = request.role
            account.touch()
            emit(id, "ROLE_CHANGED")
        }
        return account.toResponse()
    }

    @Transactional
    @CacheEvict(cacheNames = ["accounts"], key = "#p0.toString()")
    fun changeStatus(id: UUID, request: ChangeStatusRequest): AccountResponse {
        if (request.status == AccountStatus.DELETED) {
            throw InvalidAccountStateException("Use DELETE endpoint to delete an account")
        }
        val account = findActive(id)
        if (account.status != request.status) {
            account.status = request.status
            account.touch()
            emit(id, "STATUS_CHANGED")
        }
        return account.toResponse()
    }

    @Transactional
    @CacheEvict(cacheNames = ["accounts"], key = "#p0.toString()")
    fun delete(id: UUID) {
        val account = findActive(id)
        account.status = AccountStatus.DELETED
        account.touch()
        emit(id, "ACCOUNT_DELETED")
    }

    private fun findActive(id: UUID): Account = repository.findById(id)
        .orElseThrow(::AccountNotFoundException)
        .also { if (it.status == AccountStatus.DELETED) throw AccountNotFoundException() }

    private fun normalizeEmail(value: String) = value.trim().lowercase(Locale.ROOT)

    private fun emit(id: UUID, action: String) {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        events.publishEvent(AccountAuditEvent(accountId = id, action = action, performedBy = actor))
    }
}
