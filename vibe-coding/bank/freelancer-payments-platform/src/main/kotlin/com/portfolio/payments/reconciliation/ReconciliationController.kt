package com.portfolio.payments.reconciliation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reconciliation")
class ReconciliationController(private val service: ReconciliationService) {
    @GetMapping("/ledger-imbalances")
    fun ledgerImbalances() = service.detectLedgerImbalance()
}
