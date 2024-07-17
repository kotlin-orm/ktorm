
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    compileOnly(kotlin("maven-plugin"))
    compileOnly(kotlin("compiler"))
    compileOnly("org.apache.maven:maven-core:3.9.3")
    implementation("com.google.devtools.ksp:symbol-processing-cmdline:1.9.23-1.0.20")
    implementation(project(":ktorm-ksp-compiler")) {
        exclude(group = "com.pinterest.ktlint", module = "ktlint-rule-engine")
        exclude(group = "com.pinterest.ktlint", module = "ktlint-ruleset-standard")
    }
}
