
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))

    testImplementation(project(path = ":ktorm-core", configuration = "testOutput"))
    testImplementation(fileTree("lib") { include("*.jar") })
    testImplementation("org.testcontainers:oracle-xe:1.15.1")
}
