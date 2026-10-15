plugins { kotlin("plugin.spring"); kotlin("plugin.jpa") }
dependencies {
 implementation(project(":application"))
 implementation(project(":domain"))
 implementation("org.springframework.boot:spring-boot-starter-data-jpa")
 implementation("org.springframework.boot:spring-boot-starter-webflux")
 implementation("org.springframework.boot:spring-boot-starter-data-redis")
 implementation("org.springframework.kafka:spring-kafka")
 implementation("org.springframework.boot:spring-boot-starter-actuator")
 implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
 testImplementation("org.springframework.boot:spring-boot-starter-test")
 testImplementation("org.testcontainers:postgresql")
 testImplementation("org.testcontainers:kafka")
}
