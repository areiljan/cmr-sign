plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "io.github.areiljan"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "cmr-sign-core"
            pom {
                name.set("CMR Sign Core")
                description.set("Framework-free domain and signing library for CMR Sign")
                url.set("https://github.com/areiljan/cmr-sign")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/areiljan/cmr-sign/blob/main/LICENSE")
                    }
                }
            }
        }
    }
}

sourceSets.test {
    kotlin.srcDir(rootProject.file("core/src/mocks/kotlin"))
    resources.srcDir(rootProject.file("core/src/mocks/resources"))
}

dependencies {
    implementation("org.apache.pdfbox:pdfbox:3.0.4")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.86")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0-alpha-2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
