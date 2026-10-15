package com.example.trs.application

import com.example.trs.domain.*
import com.example.trs.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class RefundService(
    private val documentRepository: RefundDocumentRepository,
    private val refundRepository: RefundRepository,
    private val payoutRepository: PayoutRepository,
) {
    @Transactional
    fun request(documentId: Long, amount: BigDecimal): Refund {
        val document = documentRepository.findById(documentId).orElseThrow { NoSuchElementException("전표가 없습니다.") }
        document.purchase.requireRefundable()
        require(amount > BigDecimal.ZERO) { "환급액은 양수여야 합니다." }
        require(amount <= document.purchase.totalAmount) { "환급액이 구매액을 초과했습니다." }
        check(!refundRepository.existsByDocumentId(documentId)) { "이미 신청된 환급전표입니다." }
        return refundRepository.save(Refund(document, amount))
    }

    @Transactional
    fun approve(refundId: Long) { find(refundId).approve() }

    @Transactional
    fun preparePayout(refundId: Long): String {
        val refund = refundRepository.findByIdForUpdate(refundId) ?: throw NoSuchElementException("환급이 없습니다.")
        refund.startPayout()
        val key = "refund-${refund.id}-${UUID.randomUUID()}"
        payoutRepository.save(Payout(refund, key, refund.amount))
        // 지급기관 호출은 이 DB 트랜잭션 밖에서 별도 worker/outbox로 처리해야 합니다.
        return key
    }

    @Transactional(readOnly = true)
    fun get(refundId: Long): Refund = find(refundId)

    @Transactional(readOnly = true)
    fun search(status: RefundStatus?, merchantId: Long?): List<Refund> = refundRepository.search(status, merchantId)

    private fun find(id: Long) = refundRepository.findById(id).orElseThrow { NoSuchElementException("환급이 없습니다.") }
}
