package com.example.dbsdeposit

import com.example.dbsdeposit.config.DbsProperties
import com.example.dbsdeposit.config.IdempotencyProperties
import com.example.dbsdeposit.config.SyncProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    DbsProperties::class,
    SyncProperties::class,
    IdempotencyProperties::class,
)
class DbsDepositApplication

fun main(args: Array<String>) {
    runApplication<DbsDepositApplication>(*args)
}
