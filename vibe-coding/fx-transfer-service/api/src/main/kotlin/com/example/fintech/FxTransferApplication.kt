package com.example.fintech

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableScheduling
@EnableKafka
class FxTransferApplication

fun main(args: Array<String>) { runApplication<FxTransferApplication>(*args) }
