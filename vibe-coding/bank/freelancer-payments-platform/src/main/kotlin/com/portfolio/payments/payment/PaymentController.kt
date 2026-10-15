package com.portfolio.payments.payment

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

data class PayRequest(
    @field:NotBlank val sellerId: String,
    @field:Min(1) val amount: Long,
    @field:NotBlank val cardToken: String,
    val simulateInternalFailureAfterVanApproval: Boolean = false,
)

@RestController
@RequestMapping("/api/payments")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun pay(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: PayRequest,
    ) = paymentService.pay(
        PayCommand(
            sellerId = request.sellerId,
            amount = request.amount,
            cardToken = request.cardToken,
            idempotencyKey = idempotencyKey,
            simulateInternalFailureAfterVanApproval = request.simulateInternalFailureAfterVanApproval,
        )
    )
}
