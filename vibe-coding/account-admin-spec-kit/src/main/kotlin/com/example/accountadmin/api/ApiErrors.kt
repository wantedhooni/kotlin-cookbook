package com.example.accountadmin.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant

class AccountNotFoundException : RuntimeException("Account not found")
class DuplicateAccountException(message: String) : RuntimeException(message)
class InvalidAccountStateException(message: String) : RuntimeException(message)

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val code: String,
    val message: String,
    val path: String
)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(AccountNotFoundException::class)
    fun notFound(ex: AccountNotFoundException, req: HttpServletRequest) =
        error(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.message ?: "Not found", req)

    @ExceptionHandler(DuplicateAccountException::class, DataIntegrityViolationException::class)
    fun conflict(ex: Exception, req: HttpServletRequest) =
        error(HttpStatus.CONFLICT, "ACCOUNT_CONFLICT", "Login ID or email already exists", req)

    @ExceptionHandler(InvalidAccountStateException::class)
    fun invalidState(ex: InvalidAccountStateException, req: HttpServletRequest) =
        error(HttpStatus.BAD_REQUEST, "INVALID_STATE", ex.message ?: "Invalid state", req)

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        ConstraintViolationException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun validation(ex: Exception, req: HttpServletRequest): ResponseEntity<ApiError> {
        val message = if (ex is MethodArgumentNotValidException) {
            ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        } else "Invalid request payload or query parameter"
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, req)
    }

    private fun error(status: HttpStatus, code: String, message: String, request: HttpServletRequest) =
        ResponseEntity.status(status).body(ApiError(
            status = status.value(), code = code, message = message, path = request.requestURI
        ))
}
