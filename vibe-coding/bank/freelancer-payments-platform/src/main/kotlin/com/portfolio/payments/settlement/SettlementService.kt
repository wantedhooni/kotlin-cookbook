package com.portfolio.payments.settlement

import com.portfolio.payments.ledger.Direction
import com.portfolio.payments.ledger.LedgerEntry
import com.portfolio.payments.ledger.LedgerEntryRepository
import com.portfolio.payments.payment.PaymentRepository
import com.portfolio.payments.payment.PaymentStatus
import com.portfolio.payments.tax.TaxCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class SettlementService(
    private val payments: PaymentRepository,
    private val settlements: SettlementRepository,
    private val ledger: LedgerEntryRepository,
    private val taxCalculator: TaxCalculator,
    private val bankClient: BankClient,
) {
    @Transactional
    fun createForApprovedPayments(): List<Settlement> = payments.findAllByStatus(PaymentStatus.APPROVED).mapNotNull { p ->
        if (settlements.findByPaymentId(p.id) != null) return@mapNotNull null
        val platformFee = BigDecimal.valueOf(p.amount).multiply(BigDecimal("0.030"))
            .setScale(0, RoundingMode.DOWN).longValueExact()
        val tax = taxCalculator.calculate(p.amount)
        val payout = p.amount - platformFee - tax.withholdingTax
        val s = settlements.save(Settlement(
            id = UUID.randomUUID().toString(), paymentId = p.id, sellerId = p.sellerId,
            grossAmount = p.amount, platformFee = platformFee,
            withholdingTax = tax.withholdingTax, payoutAmount = payout,
        ))
        ledger.saveAll(listOf(
            LedgerEntry(accountKey = "PG_CLEARING", referenceId = s.id, direction = Direction.DEBIT, amount = p.amount, type = "SETTLEMENT_GROSS"),
            LedgerEntry(accountKey = "SELLER:${p.sellerId}", referenceId = s.id, direction = Direction.CREDIT, amount = payout, type = "SELLER_PAYABLE"),
            LedgerEntry(accountKey = "PLATFORM_FEE", referenceId = s.id, direction = Direction.CREDIT, amount = platformFee, type = "PLATFORM_FEE"),
            LedgerEntry(accountKey = "WITHHOLDING_TAX", referenceId = s.id, direction = Direction.CREDIT, amount = tax.withholdingTax, type = "TAX_PAYABLE"),
        ))
        s
    }

    fun payout(settlementId: String): Settlement {
        val s = settlements.findById(settlementId).orElseThrow()
        if (s.status == SettlementStatus.PAID) return s
        val response = bankClient.transfer(TransferRequest(s.id, s.sellerId, s.payoutAmount))
        return completePayout(s.id, response)
    }

    @Transactional
    fun completePayout(id: String, response: TransferResponse): Settlement {
        val s = settlements.findById(id).orElseThrow()
        if (response.accepted && response.transferId != null) s.paid(response.transferId) else s.payoutFailed()
        return s
    }
}
