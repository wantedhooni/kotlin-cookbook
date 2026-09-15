package com.example.revy.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ai")
class AiController {
    @PostMapping("/chat")
    fun chat(
        @RequestParam("question")
        question: String,
    ): String = "아직 모델과 연결되지 않았습니다."

    companion object {
        private val log = LoggerFactory.getLogger(javaClass)
    }
}
