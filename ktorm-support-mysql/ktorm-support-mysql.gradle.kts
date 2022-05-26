
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))
    api("mysql:mysql-connector-java:8.0.23")
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation(project(":ktorm-jackson"))
    testImplementation("org.testcontainers:mysql:1.15.1")
}
