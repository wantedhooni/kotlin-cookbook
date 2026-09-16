package com.example.revy.pg

@Bean
fun settlementWriter(entityManagerFactory: EntityManagerFactory): JpaItemWriter<PgSettlementEntity> =
    JpaItemWriterBuilder<PgSettlementEntity>()
        .entityManagerFactory(entityManagerFactory)
        .build()
