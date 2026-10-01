package com.example.trading.orderservice.adapter.`in`.web

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(e: RuntimeException) = ErrorResponse("INVALID_ORDER", e.message ?: "invalid order", Instant.now())

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(e: NoSuchElementException) = ErrorResponse("NOT_FOUND", e.message ?: "not found", Instant.now())
}

data class ErrorResponse(val code: String, val message: String, val timestamp: Instant)
