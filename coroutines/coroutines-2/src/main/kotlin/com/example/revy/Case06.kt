package com.example.revy

import com.example.revy.Log.printWithThread
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    printWithThread("START")
    launch {
        printWithThread("launch1 START")
        delay(600L)
        printWithThread("A")
        printWithThread("launch1 END")
    }

    printWithThread("launch1 AFTER")
    launch {
        printWithThread("launch2 START")
        delay(500L)
        throw IllegalArgumentException("코루틴 실패!")
        printWithThread("launch2 END")
    }
    printWithThread("launch2 AFTER")
    printWithThread("launch2 END")
}