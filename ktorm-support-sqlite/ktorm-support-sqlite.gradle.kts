
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))

    testImplementation(project(path = ":ktorm-core", configuration = "testOutput"))
    testImplementation("org.xerial:sqlite-jdbc:3.34.0")
}
