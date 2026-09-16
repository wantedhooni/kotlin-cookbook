package com.example.revy.pg

@Bean
fun pgSettlementJob(
    jobRepository: JobRepository,
    pgSettlementStep: Step,
): Job =
    JobBuilder(
        "pgSettlementJob",
        jobRepository,
    ).start(pgSettlementStep)
        .build()
