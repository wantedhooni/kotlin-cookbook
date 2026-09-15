package com.example.revy.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/")
class HomeController {
    @GetMapping("/")
    fun home(): String = "home"

    @GetMapping("/chat-model")
    fun chatModel(): String = "chat-model"

    @GetMapping("/chat-model-stream")
    fun chatModelStream(): String = "chat-model-stream"
}
