dependencies {
    implementation(project(":domain"))
    implementation("org.springframework:spring-tx")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.mockk:mockk:1.13.17")
}
