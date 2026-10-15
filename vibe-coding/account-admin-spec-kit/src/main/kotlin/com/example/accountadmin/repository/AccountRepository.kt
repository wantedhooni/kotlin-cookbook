package com.example.accountadmin.repository

import com.example.accountadmin.domain.Account
import com.example.accountadmin.domain.AccountRole
import com.example.accountadmin.domain.AccountStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AccountRepository : JpaRepository<Account, UUID>, AccountQueryRepository {
    fun existsByLoginId(loginId: String): Boolean
    fun existsByEmail(email: String): Boolean
}

interface AccountQueryRepository {
    fun search(keyword: String?, role: AccountRole?, status: AccountStatus?, pageable: Pageable): Page<Account>
}
