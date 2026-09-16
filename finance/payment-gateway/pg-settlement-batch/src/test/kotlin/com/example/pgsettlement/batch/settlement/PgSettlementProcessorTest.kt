package com.example.pgsettlement.batch.settlement

import com.example.pgsettlement.domain.pg.settlement.PgSettlementStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class PgSettlementProcessorTest {

    private val processor = PgSettlementProcessor()

    @Test
    fun `부분취소 정산액은 취소금액과 환급 수수료를 반영한다`() {
        val source = PgSettlementSource(
            settlementDate = LocalDate.of(2026, 9, 13),
            pgTid = "PG-TID-001",
            orderNo = "ORDER-001",
            orderId = 1L,
            orderAmount = bd("100000"),
            pgApprovedAmount = bd("100000"),
            pgCanceledAmount = bd("30000"),
            approvalAmount = bd("0"),
            cancelAmount = bd("30000"),
            feeAmount = bd("-900"),
        )

        val result = processor.process(source)!!

        assertThat(result.status).isEqualTo(PgSettlementStatus.MATCHED)
        assertThat(result.settlementAmount).isEqualByComparingTo(bd("-29100"))
        assertThat(result.settlementKey).isEqualTo("2026-09-13|PG-TID-001")
    }

    @Test
    fun `주문이 없으면 ORDER_NOT_FOUND`() {
        val source = PgSettlementSource(
            settlementDate = LocalDate.of(2026, 9, 13),
            pgTid = "PG-TID-ORPHAN",
            orderNo = "ORDER-NOT-FOUND",
            orderId = null,
            orderAmount = null,
            pgApprovedAmount = bd("50000"),
            pgCanceledAmount = bd("0"),
            approvalAmount = bd("50000"),
            cancelAmount = bd("0"),
            feeAmount = bd("1500"),
        )

        val result = processor.process(source)!!

        assertThat(result.status).isEqualTo(PgSettlementStatus.ORDER_NOT_FOUND)
        assertThat(result.settlementAmount).isEqualByComparingTo(bd("48500"))
    }

    @Test
    fun `누적 취소금액이 승인금액을 초과하면 INVALID_CANCEL_AMOUNT`() {
        val source = PgSettlementSource(
            settlementDate = LocalDate.of(2026, 9, 13),
            pgTid = "PG-TID-INVALID",
            orderNo = "ORDER-001",
            orderId = 1L,
            orderAmount = bd("100000"),
            pgApprovedAmount = bd("100000"),
            pgCanceledAmount = bd("110000"),
            approvalAmount = bd("0"),
            cancelAmount = bd("10000"),
            feeAmount = bd("-300"),
        )

        val result = processor.process(source)!!

        assertThat(result.status).isEqualTo(PgSettlementStatus.INVALID_CANCEL_AMOUNT)
    }

    private fun bd(value: String) = BigDecimal(value)
}
