plugins {
    kotlin("jvm")
}

group = "io.github.areiljan"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
}

sourceSets.test {
    kotlin.srcDir(rootProject.file("core/src/mocks/kotlin"))
    resources.srcDir(rootProject.file("core/src/mocks/resources"))
}

dependencies {
    implementation(project(":core"))

    implementation(platform("eu.europa.ec.joinup.sd-dss:dss-bom:6.5.1"))
    implementation("eu.europa.ec.joinup.sd-dss:dss-validation")
    implementation("eu.europa.ec.joinup.sd-dss:dss-pades")
    implementation("eu.europa.ec.joinup.sd-dss:dss-pades-pdfbox")
    implementation("eu.europa.ec.joinup.sd-dss:dss-policy-jaxb")
    implementation("eu.europa.ec.joinup.sd-dss:dss-cms-object")
    implementation("eu.europa.ec.joinup.sd-dss:dss-utils-apache-commons")

    testImplementation("org.bouncycastle:bcpkix-jdk18on:1.86")
    testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0-alpha-2")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

tasks.test { useJUnitPlatform() }
