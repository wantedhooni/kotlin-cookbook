package com.example.fintech.adapter.`in`.web

import com.example.fintech.application.FinanceService
import com.example.fintech.domain.DomainException
import com.example.fintech.domain.Money
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.Currency
import java.util.UUID

data class CreateAccountRequest(val currency: String, @field:DecimalMin("0.0") val initialBalance: BigDecimal = BigDecimal.ZERO)
data class MoneyMovementRequest(val fromAccountId: UUID, val toAccountId: UUID,
    @field:DecimalMin("0.0001") val amount: BigDecimal, val currency: String)
data class AccountResponse(val id: UUID, val currency: String, val balance: BigDecimal)
data class OperationResponse(val transactionId: UUID)
data class ErrorResponse(val error: String)

@RestController
@RequestMapping("/api/v1")
class FinanceController(private val service: FinanceService) {
    @PostMapping("/accounts")
    fun create(@Valid @RequestBody request: CreateAccountRequest): ResponseEntity<AccountResponse> {
        val account = service.createAccount(Currency.getInstance(request.currency), request.initialBalance)
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse(account.id, account.currency.currencyCode, account.balance))
    }
    @GetMapping("/accounts/{id}")
    fun find(@PathVariable id: UUID): AccountResponse = service.account(id).let { AccountResponse(it.id, it.currency.currencyCode, it.balance) }
    @PostMapping("/transfers")
    fun transfer(@RequestHeader("Idempotency-Key") @NotBlank key: String,
        @Valid @RequestBody request: MoneyMovementRequest): ResponseEntity<OperationResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse(service.transfer(
            request.fromAccountId, request.toAccountId, Money(request.amount, Currency.getInstance(request.currency)), key)))
    @PostMapping("/exchanges")
    fun exchange(@RequestHeader("Idempotency-Key") @NotBlank key: String,
        @Valid @RequestBody request: MoneyMovementRequest): ResponseEntity<OperationResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(OperationResponse(service.exchange(
            request.fromAccountId, request.toAccountId, Money(request.amount, Currency.getInstance(request.currency)), key)))
}

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(DomainException::class, IllegalArgumentException::class)
    fun badRequest(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(ex.message ?: "Invalid request"))
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException::class)
    fun conflict(ex: org.springframework.dao.DataIntegrityViolationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse("Request conflict; retry with same key after checking the result"))
}
