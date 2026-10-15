plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    kotlin("plugin.allopen") version "2.2.21" apply false
    kotlin("kapt") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.8" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}
allprojects {
    group = "com.example.bank"
    version = "1.0.0"
    repositories { mavenCentral() }
}
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.8") }
    }
    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
        compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
    }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}
