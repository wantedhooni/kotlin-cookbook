package com.example.revy

import com.example.revy.Log.printWithThread
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main(): Unit =
    runBlocking {
        printWithThread("runBlocking START")
        val time =
            measureTimeMillis {
                printWithThread("START")
                val job1 = async(start = CoroutineStart.LAZY) { apiCall1() }
                val job2 = async(start = CoroutineStart.LAZY) { apiCall2() }

                job1.start()
                job2.start()
                printWithThread(job1.await() + job2.await())
            }

        printWithThread("소요 시간 : $time ms")
    }

suspend fun apiCall1(): Int {
    var value = 1
    delay(1_000L)
    printWithThread("value:$value")
    return value
}

suspend fun apiCall2(): Int {
    var value = 2
    delay(1_000L)
    printWithThread("value:$value")
    return value
}
