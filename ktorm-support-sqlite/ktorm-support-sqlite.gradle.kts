
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))
    api("org.xerial:sqlite-jdbc:3.39.2.0")
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
}
