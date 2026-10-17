plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

group = "io.github.areiljan"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.apache.pdfbox:pdfbox:3.0.4")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0-alpha-2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
