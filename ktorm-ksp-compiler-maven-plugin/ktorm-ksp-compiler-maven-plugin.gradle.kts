
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
    id("org.gradlex.maven-plugin-development") version "1.0.3"
}

dependencies {
    implementation(project(":ktorm-jackson"))
    implementation(project(":ktorm-ksp-compiler"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")
    implementation("com.google.devtools.ksp:symbol-processing-common-deps:2.0.21-1.0.28")
    implementation("com.google.devtools.ksp:symbol-processing-aa-embeddable:2.0.21-1.0.28")
    compileOnly("org.apache.maven:maven-core:3.5.4")
    compileOnly("org.apache.maven:maven-artifact:3.5.4")
    compileOnly("org.apache.maven:maven-plugin-api:3.5.4")
    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.12.0")
}
