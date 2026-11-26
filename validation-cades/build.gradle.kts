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

    fun dss(module: String) = "eu.europa.ec.joinup.sd-dss:dss-$module:6.5.1"
    implementation(dss("validation"))
    implementation(dss("cades"))
    implementation(dss("policy-jaxb"))
    implementation(dss("cms-object"))
    implementation(dss("utils-apache-commons"))
    implementation(dss("service"))
    implementation(dss("crl-parser-stream"))

    testImplementation(project(":pades"))
    testImplementation(project(":efti"))
    testImplementation(project(":csc-provider"))
    testImplementation("org.bouncycastle:bcpkix-jdk18on:1.86")
    testImplementation("ch.tutteli.atrium:atrium-fluent:1.3.0-alpha-2")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.test { useJUnitPlatform() }
