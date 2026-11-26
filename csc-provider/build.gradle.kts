plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    `java-library`
    `maven-publish`
}

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0-alpha-2")
    testImplementation("org.bouncycastle:bcpkix-jdk18on:1.86")
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
