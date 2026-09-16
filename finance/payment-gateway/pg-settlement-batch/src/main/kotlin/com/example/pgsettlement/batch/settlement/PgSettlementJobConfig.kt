package com.example.pgsettlement.batch.settlement

import com.example.pgsettlement.batch.common.QuerydslPagingItemReader
import com.example.pgsettlement.common.logger
import com.example.pgsettlement.domain.pg.settlement.PgSettlementEntity
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.database.JpaItemWriter
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class PgSettlementJobConfig {
    private val log by logger()

    @Bean
    fun pgSettlementWriter(entityManagerFactory: EntityManagerFactory): JpaItemWriter<PgSettlementEntity> =
        JpaItemWriterBuilder<PgSettlementEntity>()
            .entityManagerFactory(entityManagerFactory)
            .usePersist(true)
            .build()

    @Bean
    fun pgSettlementStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        pgSettlementReader: QuerydslPagingItemReader<PgSettlementSource>,
        pgSettlementProcessor: PgSettlementProcessor,
        pgSettlementWriter: JpaItemWriter<PgSettlementEntity>,
    ): Step =
        StepBuilder("pgSettlementStep", jobRepository)
            .chunk<PgSettlementSource, PgSettlementEntity>(1_000)
            .transactionManager(transactionManager)
            .reader(pgSettlementReader)
            .processor(pgSettlementProcessor)
            .writer(pgSettlementWriter)
            .build()

    @Bean
    fun pgSettlementJob(
        jobRepository: JobRepository,
        pgSettlementStep: Step,
    ): Job =
        JobBuilder("pgSettlementJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(pgSettlementStep)
            .build()
}
