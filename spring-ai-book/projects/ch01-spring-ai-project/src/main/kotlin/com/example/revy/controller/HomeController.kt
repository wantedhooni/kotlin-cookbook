package com.example.revy.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Controller
@RequestMapping("/")
class HomeController {
    @GetMapping("/")
    fun home(): String = "home"

    companion object {
        private val log = LoggerFactory.getLogger(javaClass)
    }
}
