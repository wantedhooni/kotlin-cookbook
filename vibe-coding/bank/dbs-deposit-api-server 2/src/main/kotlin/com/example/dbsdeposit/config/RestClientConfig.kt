package com.example.dbsdeposit.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
class RestClientConfig {
    @Bean
    fun dbsRestClient(props: DbsProperties): RestClient {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(props.http.connectTimeout)
            .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(props.http.readTimeout)
        }

        return RestClient.builder()
            .baseUrl(props.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
