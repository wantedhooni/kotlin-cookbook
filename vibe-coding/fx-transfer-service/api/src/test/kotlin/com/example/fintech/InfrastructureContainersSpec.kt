package com.example.fintech

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

/** Opt-in: RUN_CONTAINER_TESTS=true gradle :api:test (Docker required). */
class InfrastructureContainersSpec : BehaviorSpec({
    Given("Docker를 사용할 수 있는 통합 테스트 환경에서") {
        When("PostgreSQL과 Kafka 컨테이너를 시작하면") {
            Then("두 서비스가 정상적으로 준비된다") {
                PostgreSQLContainer<Nothing>("postgres:16-alpine").use { pg ->
                    KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1")).use { kafka ->
                        pg.start()
                        kafka.start()
                        (pg.isRunning && kafka.isRunning) shouldBe true
                    }
                }
            }
        }
    }
})
