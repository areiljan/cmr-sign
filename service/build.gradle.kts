plugins {
    kotlin("jvm")
    application
}

group = "io.github.areiljan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    fun klite(module: String) = "com.github.keksworks.klite:klite-$module:2.0.7"
    implementation(klite("server"))
    implementation(klite("json"))
    implementation(klite("jdbc"))
    implementation(klite("slf4j"))
    implementation("org.postgresql:postgresql:42.7.13")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "io.github.areiljan.cmrsign.service.LauncherKt"
}

tasks.test { useJUnitPlatform() }
