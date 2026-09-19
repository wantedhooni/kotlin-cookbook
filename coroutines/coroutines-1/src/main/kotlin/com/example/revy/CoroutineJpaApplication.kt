package com.example.revy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoroutineJpaApplication

fun main(args: Array<String>) {
    runApplication<CoroutineJpaApplication>(*args)
}
