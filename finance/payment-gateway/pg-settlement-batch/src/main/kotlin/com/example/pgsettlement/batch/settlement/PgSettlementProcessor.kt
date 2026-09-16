package com.example.pgsettlement.batch.settlement

import com.example.pgsettlement.common.logger
import com.example.pgsettlement.domain.pg.settlement.PgSettlementEntity
import com.example.pgsettlement.domain.pg.settlement.PgSettlementStatus
import org.slf4j.LoggerFactory
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class PgSettlementProcessor : ItemProcessor<PgSettlementSource, PgSettlementEntity> {
    private val log by logger()

    override fun process(item: PgSettlementSource): PgSettlementEntity {
        val status = determineStatus(item)
        val grossAmount = item.approvalAmount - item.cancelAmount
        val settlementAmount = grossAmount - item.feeAmount

        return PgSettlementEntity(
            settlementKey = "${item.settlementDate}|${item.pgTid}",
            settlementDate = item.settlementDate,
            pgTid = item.pgTid,
            orderNo = item.orderNo,
            approvalAmount = item.approvalAmount,
            cancelAmount = item.cancelAmount,
            feeAmount = item.feeAmount,
            settlementAmount = settlementAmount,
            status = status,
        )
    }

    private fun determineStatus(item: PgSettlementSource): PgSettlementStatus =
        when {
            item.orderId == null -> PgSettlementStatus.ORDER_NOT_FOUND
            item.pgCanceledAmount > item.pgApprovedAmount -> PgSettlementStatus.INVALID_CANCEL_AMOUNT
            item.orderAmount?.compareTo(item.pgApprovedAmount) != 0 -> PgSettlementStatus.PAYMENT_AMOUNT_MISMATCH
            else -> PgSettlementStatus.MATCHED
        }
}
