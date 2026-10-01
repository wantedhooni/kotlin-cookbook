package com.example.trading.mockkrx

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class MockKrxApplication
fun main(args: Array<String>) = runApplication<MockKrxApplication>(*args)
