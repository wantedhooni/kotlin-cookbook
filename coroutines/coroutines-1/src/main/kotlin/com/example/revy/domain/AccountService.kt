package com.example.revy.domain

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class AccountService(
    private val accountRepository: AccountRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun create(
        owner: String,
        balance: BigDecimal,
    ): Account {
        log.info("create start")

        val account =
            accountRepository.save(
                Account(
                    owner = owner,
                    balance = balance,
                ),
            )

        log.info("create end: accountId={}", account.id)

        return account
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Account {
        log.info("findById start: accountId={}", id)

        val account =
            accountRepository.findById(id).orElseThrow {
                IllegalArgumentException("Account not found: $id")
            }

        log.info("findById end: accountId={}", id)

        return account
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Account> {
        log.info("findAll start")

        val accounts = accountRepository.findAll()

        log.info("findAll end: size={}", accounts.size)

        return accounts
    }
}
