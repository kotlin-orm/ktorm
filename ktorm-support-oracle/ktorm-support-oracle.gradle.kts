
plugins {
    id("ktorm.module")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation("org.testcontainers:oracle-xe:1.15.1")
    testImplementation(files("lib/ojdbc6-11.2.0.3.jar"))
}
