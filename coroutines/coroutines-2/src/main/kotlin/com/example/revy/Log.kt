package com.example.revy

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Log {
    fun printWithThread(str: Any?) {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val timestamp = current.format(formatter)

        val element = Thread.currentThread().stackTrace[2]
        val className = element.className.substringAfterLast('.') // 클래스 이름만 추출
        val methodName = element.methodName // 메서드 이름
        val lineNumber = element.lineNumber // 줄 번호

        // [클래스명.메서드명:줄번호] 메시지 형태로 출력
        println("[$timestamp][${Thread.currentThread().name}\t][$className.$methodName:$lineNumber] $str")
    }
}
