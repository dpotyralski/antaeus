plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.3.72"
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api("com.github.kagkarlsson:db-scheduler:10.3")
    api(project(":pleo-antaeus-models"))
}