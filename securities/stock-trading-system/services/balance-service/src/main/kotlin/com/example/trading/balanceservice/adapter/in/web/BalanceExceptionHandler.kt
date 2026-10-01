package com.example.trading.balanceservice.adapter.`in`.web

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class BalanceExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(e: IllegalArgumentException) = mapOf("code" to "INVALID_BALANCE_REQUEST", "message" to (e.message ?: "invalid request"))

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun conflict(e: IllegalStateException) = mapOf("code" to "BALANCE_CONFLICT", "message" to (e.message ?: "balance conflict"))
}
