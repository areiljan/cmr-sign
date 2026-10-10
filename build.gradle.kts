plugins {
    kotlin("jvm") version "2.4.20" apply false
    java
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
