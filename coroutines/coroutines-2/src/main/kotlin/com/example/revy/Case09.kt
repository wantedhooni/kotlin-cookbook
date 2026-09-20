package com.example.revy

import coroutine.UserServiceV2
import kotlinx.coroutines.delay

suspend fun main() {
    val service = UserServiceV2()
    println(service.findUser(1L))
}