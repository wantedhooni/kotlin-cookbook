package com.example.trading.accountservice.adapter.`in`.web

import com.example.trading.accountservice.application.*
import com.example.trading.domain.OrderType
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
class AccountController(private val service: AccountApplicationService) {
    @GetMapping("/api/v1/accounts/{accountId}")
    fun get(@PathVariable accountId: String) = service.get(accountId)

    @PostMapping("/internal/v1/accounts/seed")
    fun seed(@Valid @RequestBody request: SeedAccountRequest) = service.seed(request.accountId, request.cashBalance, request.creditLimit)

    @PostMapping("/internal/v1/buy-reservations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reserve(@Valid @RequestBody request: ReserveBuyRequest) {
        service.reserveBuy(request.toCommand())
    }

    @DeleteMapping("/internal/v1/buy-reservations/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun release(@PathVariable orderId: UUID) = service.releaseBuy(orderId)

    @PostMapping("/internal/v1/accounts/settlements/{executionId}")
    fun settle(@PathVariable executionId: String) = service.settleExecution(executionId)
}

data class SeedAccountRequest(
    @field:NotBlank val accountId: String,
    @field:DecimalMin("0.0") val cashBalance: BigDecimal,
    @field:DecimalMin("0.0") val creditLimit: BigDecimal = BigDecimal.ZERO,
)

data class ReserveBuyRequest(
    val orderId: UUID,
    @field:NotBlank val accountId: String,
    @field:Pattern(regexp = "\\d{6}") val symbol: String,
    val orderType: OrderType,
    @field:Min(1) val quantity: Long,
    val price: BigDecimal?,
) {
    fun toCommand() = ReserveBuyCommand(orderId, accountId, symbol, orderType, quantity, price)
}
