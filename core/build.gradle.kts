plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

sourceSets.test {
    kotlin.srcDir(rootProject.file("core/src/mocks/kotlin"))
    resources.srcDir(rootProject.file("core/src/mocks/resources"))
}

dependencies {
    testImplementation("org.bouncycastle:bcpkix-jdk18on:1.86")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0-alpha-2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
