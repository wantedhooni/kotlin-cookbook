package com.example.revy.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class AiServiceByChatClient(
    val chatClientBuilder: ChatClient.Builder,
) {
    val chatClient: ChatClient = chatClientBuilder.build()

    fun generateText(question: String): String? {
        val answers =
            chatClient
                .prompt()
                .system("사용자 질문에 대해 한국어로 답변을 해야 합니다.")
                .user(question)
                .options(
                    ChatOptions
                        .builder()
                        .temperature(0.3)
                        .maxTokens(1000)
                        .build(),
                ).call()
                .content()

        return answers
    }

    fun generateStreamText(question: String): Flux<String> {
        val answers =
            chatClient
                .prompt()
                .system("사용자 질문에 대해 한국어로 답변을 해야 합니다.")
                .user(question)
                .options(
                    ChatOptions
                        .builder()
                        .temperature(0.3)
                        .maxTokens(1000)
                        .build(),
                ).stream()
                .content()
        return answers
    }
}
