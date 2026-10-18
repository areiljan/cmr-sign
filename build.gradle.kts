plugins {
    java
    kotlin("jvm") version "2.4.21"
}

tasks.test {
    useJUnitPlatform()
    subprojects.forEach { sub ->
        sub.plugins.withId("java") {
            dependsOn("${sub.path}:testClasses")
            testClassesDirs += sub.the<SourceSetContainer>()["test"].output.classesDirs
            classpath += sub.the<SourceSetContainer>()["test"].runtimeClasspath
        }
    }
}
repositories {
    mavenCentral()
}
dependencies {
    testImplementation(kotlin("test"))
}
kotlin {
    jvmToolchain(21)
}