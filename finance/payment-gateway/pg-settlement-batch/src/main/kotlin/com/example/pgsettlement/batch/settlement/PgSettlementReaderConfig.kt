package com.example.pgsettlement.batch.settlement

import com.example.pgsettlement.batch.common.QuerydslPagingItemReader
import com.example.pgsettlement.common.logger
import com.example.pgsettlement.domain.order.QOrderEntity.orderEntity
import com.example.pgsettlement.domain.pg.payment.QPgPaymentEntity.pgPaymentEntity
import com.example.pgsettlement.domain.pg.transaction.PgTransactionType
import com.example.pgsettlement.domain.pg.transaction.QPgTransactionEntity.pgTransactionEntity
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.CaseBuilder
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal
import java.time.LocalDate

@Configuration
class PgSettlementReaderConfig {
    private val log by logger()

    @Bean
    @StepScope
    fun pgSettlementReader(
        entityManagerFactory: EntityManagerFactory,
        @Value("#{jobParameters['settlementDate']}") settlementDateParameter: String,
    ): QuerydslPagingItemReader<PgSettlementSource> {
        log.info("PG 정산 리더 시작. settlementDate={}", settlementDateParameter)
        val settlementDate = LocalDate.parse(settlementDateParameter)
        val tx = pgTransactionEntity
        val payment = pgPaymentEntity
        val order = orderEntity

        val approvalAmount =
            CaseBuilder()
                .`when`(tx.type.eq(PgTransactionType.APPROVAL))
                .then(tx.amount)
                .otherwise(BigDecimal.ZERO)
                .sum()
                .coalesce(BigDecimal.ZERO)

        val cancelAmount =
            CaseBuilder()
                .`when`(
                    tx.type.`in`(
                        PgTransactionType.PARTIAL_CANCEL,
                        PgTransactionType.CANCEL,
                    ),
                ).then(tx.amount)
                .otherwise(BigDecimal.ZERO)
                .sum()
                .coalesce(BigDecimal.ZERO)

        val feeAmount = tx.feeAmount.sum().coalesce(BigDecimal.ZERO)

        return QuerydslPagingItemReader(
            entityManagerFactory = entityManagerFactory,
            name = "pgSettlementReader",
            pageSize = 1_000,
        ) { queryFactory ->
            queryFactory
                .select(
                    Projections.constructor(
                        PgSettlementSource::class.java,
                        tx.settlementDate,
                        payment.pgTid,
                        payment.orderNo,
                        order.id,
                        order.orderAmount,
                        payment.approvedAmount,
                        payment.canceledAmount,
                        approvalAmount,
                        cancelAmount,
                        feeAmount,
                    ),
                ).from(tx)
                .join(tx.payment, payment)
                .leftJoin(payment.order, order)
                .where(tx.settlementDate.eq(settlementDate))
                .groupBy(
                    tx.settlementDate,
                    payment.pgTid,
                    payment.orderNo,
                    order.id,
                    order.orderAmount,
                    payment.approvedAmount,
                    payment.canceledAmount,
                )
                // pg_tid는 unique. Paging 결과가 흔들리지 않도록 유일한 정렬 기준을 사용한다.
                .orderBy(payment.pgTid.asc())
        }
    }
}
