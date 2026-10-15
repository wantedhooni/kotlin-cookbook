package com.example.dbsdeposit.api

import com.example.dbsdeposit.support.ConcurrentSyncException
import com.example.dbsdeposit.support.IdempotencyInProgressException
import com.example.dbsdeposit.support.IdempotencyKeyConflictException
import com.example.dbsdeposit.support.ProviderCallException
import com.example.dbsdeposit.support.ResourceNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.time.Instant

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun notFound(e: ResourceNotFoundException, request: HttpServletRequest) =
        problem(HttpStatus.NOT_FOUND, "NOT_FOUND", e.message ?: "Not found", request)

    @ExceptionHandler(IdempotencyKeyConflictException::class)
    fun idempotencyConflict(e: IdempotencyKeyConflictException, request: HttpServletRequest) =
        problem(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED", e.message ?: "Idempotency key conflict", request)

    @ExceptionHandler(IdempotencyInProgressException::class)
    fun idempotencyProcessing(e: IdempotencyInProgressException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "IDEMPOTENCY_IN_PROGRESS", e.message ?: "Request in progress", request, retryAfter = "1")

    @ExceptionHandler(ConcurrentSyncException::class)
    fun concurrentSync(e: ConcurrentSyncException, request: HttpServletRequest): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "SYNC_ALREADY_RUNNING", e.message ?: "Sync already running", request, retryAfter = "1")

    @ExceptionHandler(ProviderCallException::class)
    fun provider(e: ProviderCallException, request: HttpServletRequest) =
        problem(HttpStatus.BAD_GATEWAY, "DBS_PROVIDER_ERROR", e.message ?: "DBS provider error", request)

    @ExceptionHandler(MissingRequestHeaderException::class, IllegalArgumentException::class)
    fun badRequest(e: Exception, request: HttpServletRequest) =
        problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.message ?: "Bad request", request)

    @ExceptionHandler(Exception::class)
    fun unexpected(e: Exception, request: HttpServletRequest) =
        problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", request)

    private fun problem(
        status: HttpStatus,
        code: String,
        detail: String,
        request: HttpServletRequest,
        retryAfter: String? = null,
    ): ResponseEntity<ProblemDetail> {
        val body = ProblemDetail.forStatusAndDetail(status, detail).apply {
            title = code
            instance = URI.create(request.requestURI)
            setProperty("timestamp", Instant.now())
        }
        val builder = ResponseEntity.status(status)
        if (retryAfter != null) builder.header("Retry-After", retryAfter)
        return builder.body(body)
    }
}
