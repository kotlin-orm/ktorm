
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation(project(":ktorm-jackson"))
    testImplementation("mysql:mysql-connector-java:8.0.23")
    testImplementation("org.testcontainers:mysql:1.15.1")
}
