package com.example.revy.pg

@Bean
fun pgSettlementStep(
    jobRepository: JobRepository,
    transactionManager: PlatformTransactionManager,
    pgPaymentReader: JpaPagingItemReader<PgPaymentEntity>,
    settlementProcessor: ItemProcessor<PgPaymentEntity, PgSettlementEntity>,
    settlementWriter: JpaItemWriter<PgSettlementEntity>,
): Step =
    StepBuilder(
        "pgSettlementStep",
        jobRepository,
    ).chunk<PgPaymentEntity, PgSettlementEntity>(1000)
        .transactionManager(transactionManager)
        .reader(pgPaymentReader)
        .processor(settlementProcessor)
        .writer(settlementWriter)
        .build()
