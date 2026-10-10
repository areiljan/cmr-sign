plugins {
    kotlin("jvm")
    application
}

group = "io.github.areiljan"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "io.github.areiljan.cmrsign.service.MainKt"
}
