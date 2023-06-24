
plugins {
    id("ktorm.module")
}

dependencies {
    compileOnly(project(":ktorm-core"))
    compileOnly(project(":ktorm-jackson"))
}
