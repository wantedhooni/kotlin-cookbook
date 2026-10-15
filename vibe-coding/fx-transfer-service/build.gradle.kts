plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    id("org.springframework.boot") version "3.5.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    jacoco
}
allprojects { group = "com.example.fintech"; version = "0.2.0"; repositories { mavenCentral() } }
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")
    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.7") }
    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
    dependencies {
        add("testImplementation", "io.kotest:kotest-runner-junit5:5.9.1")
        add("testImplementation", "io.kotest:kotest-assertions-core:5.9.1")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }
    tasks.withType<Test>().configureEach {
        if (System.getenv("RUN_CONTAINER_TESTS") != "true") {
            exclude("**/BankSagaIntegrationSpec.class", "**/InfrastructureContainersSpec.class")
        }
    }
}
