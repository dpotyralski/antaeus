plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation("com.github.kagkarlsson:db-scheduler:10.3")
    api(project(":pleo-antaeus-models"))
}