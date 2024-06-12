
plugins {
    id("ktorm.base")
    id("ktorm.modularity")
    id("ktorm.publish")
    id("ktorm.source-header-check")
    id("ktorm.tuples-codegen")
}

dependencies {
    compileOnly("org.springframework:spring-jdbc:5.0.10.RELEASE")
    compileOnly("org.springframework:spring-tx:5.0.10.RELEASE")
    testImplementation("com.h2database:h2:1.4.198")
    testImplementation("org.slf4j:slf4j-simple:2.0.3")
}

val testOutput by configurations.creating {
    extendsFrom(configurations["testImplementation"])
}

val testJar by tasks.registering(Jar::class) {
    dependsOn("testClasses")
    from(sourceSets.test.map { it.output })
    archiveClassifier.set("test")
}

artifacts {
    add(testOutput.name, testJar)
}
