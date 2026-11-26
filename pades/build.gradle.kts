plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
}

sourceSets.test {
    kotlin.srcDir(rootProject.file("core/src/mocks/kotlin"))
    resources.srcDir(rootProject.file("core/src/mocks/resources"))
}

dependencies {
    api(project(":core"))
    implementation("org.apache.pdfbox:pdfbox:2.0.37")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.86")

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
