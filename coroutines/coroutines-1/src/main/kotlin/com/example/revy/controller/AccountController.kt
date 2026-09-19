package com.example.revy.controller

import com.example.revy.domain.Account
import com.example.revy.domain.AccountService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val accountService: AccountService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateAccountRequest,
    ): AccountResponse {
        log.info("create request")

        return accountService
            .create(
                owner = request.owner,
                balance = request.balance,
            ).toResponse()
    }

    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: Long,
    ): AccountResponse {
        log.info("findById request: accountId={}", id)

        return accountService
            .findById(id)
            .toResponse()
    }

    @GetMapping
    fun findAll(): List<AccountResponse> {
        log.info("findAll request")

        return accountService
            .findAll()
            .map { it.toResponse() }
    }
}

data class CreateAccountRequest(
    val owner: String,
    val balance: BigDecimal,
)

data class AccountResponse(
    val id: Long,
    val owner: String,
    val balance: BigDecimal,
)

private fun Account.toResponse() =
    AccountResponse(
        id = id,
        owner = owner,
        balance = balance,
    )
