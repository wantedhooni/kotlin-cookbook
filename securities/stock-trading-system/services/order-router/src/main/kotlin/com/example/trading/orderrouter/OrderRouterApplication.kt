package com.example.trading.orderrouter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.Clock
import java.time.ZoneId
import org.springframework.context.annotation.Bean

@SpringBootApplication
class OrderRouterApplication {
    @Bean fun tradingClock(): Clock = Clock.system(ZoneId.of("Asia/Seoul"))
}
fun main(args: Array<String>) = runApplication<OrderRouterApplication>(*args)
