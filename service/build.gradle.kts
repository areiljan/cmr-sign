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
    implementation(project(":core"))

    fun klite(module: String) = "com.github.keksworks.klite:klite-$module:2.0.7"
    implementation(klite("server"))
    implementation(klite("json"))
    implementation(klite("jdbc"))
    implementation(klite("slf4j"))
    implementation("org.postgresql:postgresql:42.7.13")

    testImplementation(klite("jdbc-test"))
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0-alpha-2")
}

application {
    mainClass = "io.github.areiljan.cmrsign.service.LauncherKt"
}

tasks.test {
    useJUnitPlatform()
    workingDir(rootDir)
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}
