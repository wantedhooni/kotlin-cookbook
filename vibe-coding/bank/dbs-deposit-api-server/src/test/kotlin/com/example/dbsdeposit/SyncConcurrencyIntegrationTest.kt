package com.example.dbsdeposit

import com.example.dbsdeposit.service.DepositSyncService
import com.example.dbsdeposit.support.ConcurrentSyncException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(properties = ["dbs.mock-delay=500ms"])
class SyncConcurrencyIntegrationTest(
    @Autowired private val syncService: DepositSyncService,
) {
    @Test
    fun `only one sync per party can own the distributed lease`() {
        val pool = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        try {
            val futures = (1..2).map { idx ->
                pool.submit<String> {
                    start.await()
                    try {
                        syncService.sync("concurrent-party", "concurrent-$idx")
                        "OK"
                    } catch (_: ConcurrentSyncException) {
                        "CONFLICT"
                    }
                }
            }
            start.countDown()
            val results = futures.map { it.get(5, TimeUnit.SECONDS) }.sorted()
            assertEquals(listOf("CONFLICT", "OK"), results)
        } finally {
            pool.shutdownNow()
        }
    }
}
