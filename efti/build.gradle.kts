plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

group = "io.github.areiljan"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core"))

    testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0-alpha-2")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.test { useJUnitPlatform() }
