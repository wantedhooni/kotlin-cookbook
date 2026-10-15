package com.portfolio.payments.settlement

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/settlements")
class SettlementController(private val service: SettlementService) {
    @PostMapping("/run")
    fun run() = service.createForApprovedPayments()

    @PostMapping("/{id}/payout")
    fun payout(@PathVariable id: String) = service.payout(id)
}
