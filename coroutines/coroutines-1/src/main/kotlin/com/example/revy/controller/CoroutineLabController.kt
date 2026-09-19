package com.example.revy.controller

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/labs/coroutines")
class CoroutineLabController {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/blocking")
    fun blocking(): String {
        log.info("blocking start")

        Thread.sleep(3_000)

        log.info("blocking end")

        return "blocking"
    }

    @GetMapping("/suspend-blocking")
    suspend fun suspendBlocking(): String {
        log.info("suspend-blocking start")

        Thread.sleep(3_000)

        log.info("suspend-blocking end")

        return "suspend-blocking"
    }

    @GetMapping("/suspend-delay")
    suspend fun suspendDelay(): String {
        log.info("suspend-delay start")

        delay(3_000)

        log.info("suspend-delay end")

        return "suspend-delay"
    }
}
