
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))
    api("com.microsoft.sqlserver:mssql-jdbc:7.2.2.jre8")
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation("org.testcontainers:mssqlserver:1.15.1")
}
