package com.example.revy.controller

import com.example.revy.service.AiService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/ai")
class AiController(
    val aiService: AiService,
) {
    @PostMapping("/chat-model")
    fun chatModel(
        @RequestParam("question") question: String,
    ): String = aiService.generateText(question)

    @PostMapping("/chat-model-stream")
    fun chatModelStream(
        @RequestParam("question") question: String,
    ): Flux<String> = aiService.generateStreamText(question)
}
