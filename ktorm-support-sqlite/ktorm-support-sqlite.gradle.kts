
plugins {
    id("ktorm.module")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation("org.xerial:sqlite-jdbc:3.39.2.0")
}
