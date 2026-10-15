package com.example.revy

import java.time.Instant

class InstantTest {
}

fun main() {
 println("Instant.now(): ${Instant.now()}")
 println("Instant.now().epochSecond: ${Instant.now().epochSecond}")

 val array =  arrayOf("apple", "banana", "orange")

 array.forEach {
  println(it)
 }


val result = array.filter { it.startsWith("a") }
 println(result)



}