package com.example.revy.controller

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class CoroutineBehaviorTest {
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun `Thread sleep은 현재 thread를 blocking 한다`() {
        log.info("before sleep")

        Thread.sleep(1_000)

        log.info("after sleep")
    }

    @Test
    fun `delay는 coroutine을 suspend 한다`() =
        runTest {
            log.info("before delay")

            delay(1_000)

            log.info("after delay")
        }
}
