package com.example.trading.balanceservice.adapter.`in`.web

import com.example.trading.balanceservice.application.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
class BalanceController(private val service: BalanceApplicationService) {
    @GetMapping("/api/v1/balances/{accountId}/{symbol}")
    fun get(@PathVariable accountId: String, @PathVariable symbol: String) = service.get(accountId, symbol)

    @GetMapping("/api/v1/balances/{accountId}")
    fun list(@PathVariable accountId: String) = service.list(accountId)

    @PostMapping("/internal/v1/positions/seed")
    fun seed(@Valid @RequestBody request: SeedPositionRequest) = service.seed(request.accountId, request.symbol, request.quantity, request.averagePrice)

    @PostMapping("/internal/v1/sell-reservations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reserve(@Valid @RequestBody request: ReserveSellRequest) = service.reserveSell(request.orderId, request.accountId, request.symbol, request.quantity)

    @DeleteMapping("/internal/v1/sell-reservations/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun release(@PathVariable orderId: UUID) = service.releaseSell(orderId)
}

data class SeedPositionRequest(
    @field:NotBlank val accountId: String,
    @field:Pattern(regexp = "\\d{6}") val symbol: String,
    @field:Min(0) val quantity: Long,
    val averagePrice: BigDecimal,
)

data class ReserveSellRequest(
    val orderId: UUID,
    @field:NotBlank val accountId: String,
    @field:Pattern(regexp = "\\d{6}") val symbol: String,
    @field:Min(1) val quantity: Long,
)
