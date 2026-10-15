package com.example.fintech.adapter.`in`.web

import com.example.fintech.adapter.out.bank.BankTransferSaga
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

data class BankPayoutRequest(val sourceAccountId: UUID, @field:NotBlank val bankCode: String,
 @field:NotBlank val destinationAccount: String, @field:DecimalMin("0.0001") val amount: BigDecimal,
 @field:NotBlank val currency: String)
@RestController
@RequestMapping("/api/v1/bank-transfers")
class BankTransferController(private val saga: BankTransferSaga) {
 @PostMapping
 fun initiate(@RequestHeader("Idempotency-Key") key: String, @Valid @RequestBody request: BankPayoutRequest): ResponseEntity<BankTransferSaga.State> =
  ResponseEntity.status(HttpStatus.ACCEPTED).body(saga.initiate(BankTransferSaga.Command(request.sourceAccountId,request.bankCode,request.destinationAccount,request.amount,request.currency),key))
 @GetMapping("/{id}") fun status(@PathVariable id: UUID) = saga.find(id)
 // No public reconciliation endpoint: resolve only after authenticated bank evidence/privileged backoffice process.
}
