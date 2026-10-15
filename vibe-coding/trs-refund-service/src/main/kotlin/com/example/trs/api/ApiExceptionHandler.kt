package com.example.trs.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(NoSuchElementException::class)
    fun notFound(e: NoSuchElementException) = ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (e.message ?: "Not found")))

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException) = ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Bad request")))

    @ExceptionHandler(IllegalStateException::class)
    fun conflict(e: IllegalStateException) = ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (e.message ?: "Conflict")))
}
