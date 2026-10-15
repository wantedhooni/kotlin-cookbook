package com.example.bank.api

import com.example.bank.application.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages=["com.example.bank"])
@EnableScheduling
@EntityScan("com.example.bank.infrastructure")
@EnableJpaRepositories("com.example.bank.infrastructure")
class BankApplication {
    @Bean fun bankService(accounts:AccountPort,transfers:TransferPort,ledger:LedgerPort,events:EventPort, policy:TransferPolicyPort) =
        BankService(accounts,transfers,ledger,events,policy)
}
fun main(args:Array<String>) { runApplication<BankApplication>(*args) }
