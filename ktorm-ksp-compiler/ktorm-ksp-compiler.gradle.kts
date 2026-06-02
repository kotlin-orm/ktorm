
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    implementation(project(":ktorm-core"))
    implementation(project(":ktorm-ksp-annotations"))
    implementation(project(":ktorm-ksp-spi"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.7")
    implementation("com.squareup:kotlinpoet-ksp:2.3.0")
    implementation("org.atteo:evo-inflector:1.3")
    testImplementation("dev.zacsweers.kctfork:core:0.12.1")
    testImplementation("dev.zacsweers.kctfork:ksp:0.12.1")
    testImplementation("com.h2database:h2:1.4.198")
    testImplementation("org.slf4j:slf4j-simple:2.0.3")
}

if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
    tasks.test {
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }
}
