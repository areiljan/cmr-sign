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
subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            coreLibrariesVersion = "2.2.21"
            compilerOptions.apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        }
    }
}
