
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation(files("lib/ojdbc6-11.2.0.3.jar"))
    testImplementation("org.testcontainers:oracle-xe:1.15.1")
}
