package com.example.trading.accountservice.adapter.`in`.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AccountExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class, NoSuchElementException::class)
    fun handle(e: RuntimeException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(if (e is NoSuchElementException) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to (e.message ?: "request failed")))
}
