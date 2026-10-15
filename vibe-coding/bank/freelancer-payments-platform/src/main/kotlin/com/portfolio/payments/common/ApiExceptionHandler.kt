package com.portfolio.payments.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

class DomainException(message: String) : RuntimeException(message)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(DomainException::class)
    fun handleDomain(ex: DomainException) = ResponseEntity.status(HttpStatus.CONFLICT).body(
        mapOf("timestamp" to Instant.now().toString(), "error" to (ex.message ?: "domain error"))
    )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception) = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        mapOf("timestamp" to Instant.now().toString(), "error" to "internal error", "detail" to (ex.message ?: "unknown"))
    )
}
