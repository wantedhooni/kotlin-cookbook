package com.example.trading.orderservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class OrderServiceApplication
fun main(args: Array<String>) = runApplication<OrderServiceApplication>(*args)
