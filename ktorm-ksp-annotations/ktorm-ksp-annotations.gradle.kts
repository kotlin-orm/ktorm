
plugins {
    id("ktorm.base")
    id("ktorm.modularity")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    api(project(":ktorm-core"))
}
