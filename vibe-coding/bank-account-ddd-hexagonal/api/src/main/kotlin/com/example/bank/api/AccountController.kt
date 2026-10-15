package com.example.bank.api

import com.example.bank.application.*
import com.example.bank.domain.*
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

data class OpenAccountRequest(@field:NotBlank val bankCode:String,@field:NotBlank val accountNumber:String,
                              @field:NotBlank val ownerName:String)
data class MoneyRequest(@field:DecimalMin("0.01") @field:Digits(integer=17,fraction=2) val amount:BigDecimal)
data class TransferRequest(val requestId:UUID,val fromAccountId:Long,val toAccountId:Long,
                           @field:DecimalMin("0.01") @field:Digits(integer=17,fraction=2) val amount:BigDecimal)
data class ApiError(val code:String,val message:String)
@RestController @RequestMapping("/api/v1/accounts")
class AccountController(private val service:BankService, private val audit:AuditPort) {
    private fun owns(id:Long, jwt:Jwt) {
        if(service.get(id).ownerName != jwt.subject) throw BankException("FORBIDDEN", "Not your account")
    }
    @PostMapping fun open(@AuthenticationPrincipal jwt:Jwt,@Valid @RequestBody body:OpenAccountRequest):ResponseEntity<Account> {
        if(body.ownerName != jwt.subject) throw BankException("FORBIDDEN", "Owner must match authenticated subject")
        val account=service.open(body.bankCode,body.accountNumber,body.ownerName)
        audit.record(jwt.subject,"ACCOUNT_OPENED",account.id.toString(),"created")
        return ResponseEntity.status(HttpStatus.CREATED).body(account)
    }
    @GetMapping("/{id}") fun get(@AuthenticationPrincipal jwt:Jwt,@PathVariable id:Long):Account { owns(id,jwt);return service.get(id) }
    @GetMapping fun search(@AuthenticationPrincipal jwt:Jwt,@RequestParam(required=false) bankCode:String?,
                          @RequestParam(required=false) status:AccountStatus?)=service.search(bankCode,jwt.subject,status)
    @GetMapping("/{id}/transactions") fun history(@AuthenticationPrincipal jwt:Jwt,@PathVariable id:Long):List<TransactionRecord> {
        owns(id,jwt);return service.history(id)
    }
    @PostMapping("/{id}/deposits") fun deposit(@AuthenticationPrincipal jwt:Jwt,@PathVariable id:Long,
                                               @Valid @RequestBody body:MoneyRequest):Account {
        owns(id,jwt);val result=service.deposit(id,body.amount)
        audit.record(jwt.subject,"DEPOSIT",id.toString(),body.amount.toPlainString());return result
    }
    @PostMapping("/{id}/withdrawals") fun withdraw(@AuthenticationPrincipal jwt:Jwt,@PathVariable id:Long,
                                                    @Valid @RequestBody body:MoneyRequest):Account {
        owns(id,jwt);val result=service.withdraw(id,body.amount)
        audit.record(jwt.subject,"WITHDRAWAL",id.toString(),body.amount.toPlainString());return result
    }
    @PostMapping("/transfers") fun transfer(@AuthenticationPrincipal jwt:Jwt,@Valid @RequestBody body:TransferRequest):TransferResult {
        owns(body.fromAccountId,jwt)
        val result=service.transfer(TransferCommand(body.requestId,body.fromAccountId,body.toAccountId,body.amount))
        audit.record(jwt.subject,"TRANSFER",body.fromAccountId.toString(),"request=${body.requestId};target=${body.toAccountId};replayed=${result.replayed}")
        return result
    }
}
@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(BankException::class)
    fun business(e:BankException):ResponseEntity<ApiError> {
        val status=when(e.code) {
            "FORBIDDEN"->HttpStatus.FORBIDDEN
            "ACCOUNT_NOT_FOUND","BANK_NOT_FOUND"->HttpStatus.NOT_FOUND
            "INSUFFICIENT_FUNDS","DAILY_LIMIT_EXCEEDED","DUPLICATE_ACCOUNT","ACCOUNT_CLOSED","SAME_ACCOUNT","IDEMPOTENCY_CONFLICT","TRANSFER_PENDING"->HttpStatus.CONFLICT
            else->HttpStatus.BAD_REQUEST
        }
        return ResponseEntity.status(status).body(ApiError(e.code,e.message))
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException::class)
    fun constraint(e:Exception)=ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError("DATA_CONFLICT","Constraint violated"))
}
