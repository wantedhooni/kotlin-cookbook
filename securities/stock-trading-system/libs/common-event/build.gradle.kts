plugins {
    kotlin("jvm")
    jacoco
}

dependencies { implementation(project(":libs:common-domain")) }
kotlin { jvmToolchain(21) }
