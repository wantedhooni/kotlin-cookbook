package com.example.pgsettlement.batch.config

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository
import org.springframework.context.annotation.Configuration

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository(
    dataSourceRef = "dataSource",
    transactionManagerRef = "transactionManager",
    tablePrefix = "BATCH_",
)
class BatchConfiguration
