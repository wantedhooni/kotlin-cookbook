package com.example.revy.service

import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class AiService(
    val chatModel: ChatModel,
) {
    fun generateText(question: String): String {
        // 시스템 메시지
        val systemMessage =
            SystemMessage
                .builder()
                .text(
                    "사용자 질문에 대해 한국어로 답변을 해야 합니다.",
                ).build()
        // 사용자 메시지
        val userMessage = UserMessage.builder().text(question).build()
        // 대화 옵션
        val chatOptions =
            ChatOptions
                .builder()
                .model("gpt-4o-mini")
                .temperature(0.3)
                .maxTokens(1000)
                .build()
        // 프롬프트
        val prompt =
            Prompt
                .builder()
                .messages(systemMessage, userMessage)
                .chatOptions(chatOptions)
                .build()

        // LLM 요청 및 응답
        val chatResponse = chatModel.call(prompt)
        val assistantMessage = chatResponse.result.output
        val answer = assistantMessage.text
        return answer.toString()
    }

    fun generateStreamText(question: String): Flux<String> {
        // 시스템 메시지
        val systemMessage =
            SystemMessage
                .builder()
                .text(
                    "사용자 질문에 대해 한국어로 답변을 해야 합니다.",
                ).build()
        // 사용자 메시지
        val userMessage = UserMessage.builder().text(question).build()
        // 대화 옵션
        val chatOptions =
            ChatOptions
                .builder()
                .model("gpt-4o-mini")
                .temperature(0.3)
                .maxTokens(1000)
                .build()
        // 프롬프트
        val prompt =
            Prompt
                .builder()
                .messages(systemMessage, userMessage)
                .chatOptions(chatOptions)
                .build()

        // LLM 요청 및 응답
        val fluxResponse = chatModel.stream(prompt)
        val fluxString =
            fluxResponse.map {
                it.result.output.text ?: ""
            }
        return fluxString
    }
}
