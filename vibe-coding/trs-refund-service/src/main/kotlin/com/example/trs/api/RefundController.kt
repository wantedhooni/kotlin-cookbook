package com.example.trs.api

import com.example.trs.application.RefundService
import com.example.trs.domain.Refund
import com.example.trs.domain.RefundStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

 data class RefundRequest(@field:DecimalMin("1") val amount: BigDecimal)
 data class RefundResponse(val id: Long, val documentId: Long, val amount: BigDecimal, val status: RefundStatus) {
    companion object { fun from(refund: Refund) = RefundResponse(refund.id!!, refund.document.id!!, refund.amount, refund.status) }
 }
 data class PayoutPreparedResponse(val idempotencyKey: String)

@RestController
@RequestMapping("/api/v1/refunds")
class RefundController(private val service: RefundService) {
    @PostMapping("/documents/{documentId}", consumes = ["application/json"])
    @ResponseStatus(HttpStatus.CREATED)
    fun request(@PathVariable documentId: Long, @Valid @RequestBody request: RefundRequest): RefundResponse =
        RefundResponse.from(service.request(documentId, request.amount))

    @PostMapping("/{refundId}/approve")
    fun approve(@PathVariable refundId: Long) = service.approve(refundId)

    @PostMapping("/{refundId}/prepare-payout")
    fun preparePayout(@PathVariable refundId: Long): PayoutPreparedResponse =
        PayoutPreparedResponse(service.preparePayout(refundId))

    @GetMapping("/{refundId}")
    fun get(@PathVariable refundId: Long): RefundResponse = RefundResponse.from(service.get(refundId))

    @GetMapping
    fun search(@RequestParam(required = false) status: RefundStatus?, @RequestParam(required = false) merchantId: Long?): List<RefundResponse> =
        service.search(status, merchantId).map(RefundResponse::from)
}
