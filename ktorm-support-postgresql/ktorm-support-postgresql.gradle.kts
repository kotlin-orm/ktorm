
plugins {
    id("ktorm.base")
    id("ktorm.modularity")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation(project(":ktorm-jackson"))
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("org.postgresql:postgresql:42.2.5")
    testImplementation("com.zaxxer:HikariCP:4.0.3")
    testImplementation("com.mchange:c3p0:0.9.5.5")
    testImplementation("org.apache.commons:commons-dbcp2:2.8.0")
    testImplementation("com.alibaba:druid:1.2.6")
}
