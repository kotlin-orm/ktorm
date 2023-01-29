
plugins {
    id("ktorm.module")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation(project(":ktorm-jackson"))
    testImplementation("org.testcontainers:mysql:1.15.1")
    testImplementation("mysql:mysql-connector-java:8.0.23")
}
