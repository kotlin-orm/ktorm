
plugins {
    id("ktorm.base")
    id("ktorm.modularity")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation("org.xerial:sqlite-jdbc:3.39.2.0")
}
