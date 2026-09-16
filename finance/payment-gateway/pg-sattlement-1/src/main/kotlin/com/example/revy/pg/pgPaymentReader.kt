package com.example.revy.pg

import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import java.time.LocalDate

@Bean
@StepScope
fun pgPaymentReader(
    entityManagerFactory: EntityManagerFactory,
    @Value("#{jobParameters['settlementDate']}")
    settlementDate: String,
): JpaPagingItemReader<PgPaymentEntity> {
    val date = LocalDate.parse(settlementDate)

    return JpaPagingItemReaderBuilder<PgPaymentEntity>()
        .name("pgPaymentReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString(
            """
            select p
            from PgPaymentEntity p
            left join fetch p.order o
            where p.approvedAt >= :from
              and p.approvedAt < :to
            order by p.id
            """.trimIndent(),
        ).parameterValues(
            mapOf(
                "from" to date.atStartOfDay(),
                "to" to date.plusDays(1).atStartOfDay(),
            ),
        ).pageSize(1000)
        .build()
}
