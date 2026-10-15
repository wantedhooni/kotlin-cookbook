package com.portfolio.payments.reconciliation

import com.portfolio.payments.ledger.Direction
import com.portfolio.payments.ledger.LedgerEntryRepository
import com.portfolio.payments.settlement.SettlementRepository
import org.springframework.stereotype.Service

data class ReconciliationIssue(val settlementId: String, val debit: Long, val credit: Long)

@Service
class ReconciliationService(
    private val settlements: SettlementRepository,
    private val ledger: LedgerEntryRepository,
) {
    fun detectLedgerImbalance(): List<ReconciliationIssue> = settlements.findAll().mapNotNull { s ->
        val entries = ledger.findAllByReferenceId(s.id)
        val debit = entries.filter { it.direction == Direction.DEBIT }.sumOf { it.amount }
        val credit = entries.filter { it.direction == Direction.CREDIT }.sumOf { it.amount }
        if (debit == credit) null else ReconciliationIssue(s.id, debit, credit)
    }
}
