
plugins {
    id("ktorm.module")
}

dependencies {
    compileOnly(project(":ktorm-core"))
    testImplementation(project(":ktorm-core"))
}
