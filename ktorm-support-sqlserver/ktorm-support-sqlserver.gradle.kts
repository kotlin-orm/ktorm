
plugins {
    id("ktorm.base")
    id("ktorm.modularity")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation("org.testcontainers:mssqlserver:1.15.1")
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:7.2.2.jre8")
}
