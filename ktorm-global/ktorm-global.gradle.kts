
plugins {
    id("ktorm.module")
}

dependencies {
    api(project(":ktorm-core"))
    compileOnly("org.springframework:spring-jdbc:5.0.10.RELEASE")
    compileOnly("org.springframework:spring-tx:5.0.10.RELEASE")
    testImplementation(project(":ktorm-core", configuration = "testOutput"))
    testImplementation("com.h2database:h2:1.4.198")
}
