package com.example.trading.executionservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class ExecutionServiceApplication
fun main(args: Array<String>) = runApplication<ExecutionServiceApplication>(*args)
