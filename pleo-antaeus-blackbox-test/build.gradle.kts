plugins {
    kotlin("jvm")
}

kotlinProject()


val blackboxTestsTask = tasks.register<Test>("blackboxTests") {
    useJUnitPlatform {
        includeTags("black-box")
    }
}

dependencies {
    testImplementation("org.testcontainers:testcontainers:1.16.0")
    testImplementation("io.rest-assured:rest-assured:4.4.0")
}