package com.example.accountadmin.repository

import com.example.accountadmin.domain.Account
import com.example.accountadmin.domain.AccountRole
import com.example.accountadmin.domain.AccountStatus
import com.example.accountadmin.domain.QAccount.account
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class AccountQueryRepositoryImpl(entityManager: EntityManager) : AccountQueryRepository {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun search(
        keyword: String?, role: AccountRole?, status: AccountStatus?, pageable: Pageable
    ): Page<Account> {
        val predicates = BooleanBuilder()
        if (status != null) predicates.and(account.status.eq(status))
        else predicates.and(account.status.ne(AccountStatus.DELETED))
        role?.let { predicates.and(account.role.eq(it)) }
        keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
            predicates.and(
                account.loginId.containsIgnoreCase(value)
                    .or(account.email.containsIgnoreCase(value))
                    .or(account.displayName.containsIgnoreCase(value))
            )
        }
        val items = queryFactory.selectFrom(account)
            .where(predicates)
            .orderBy(account.createdAt.desc(), account.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val total = queryFactory.select(account.count()).from(account)
            .where(predicates).fetchOne() ?: 0L
        return PageImpl(items, pageable, total)
    }
}
