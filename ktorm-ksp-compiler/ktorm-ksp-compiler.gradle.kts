
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    implementation(project(":ktorm-core"))
    implementation(project(":ktorm-ksp-annotations"))
    implementation(project(":ktorm-ksp-spi"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")
    implementation("com.squareup:kotlinpoet-ksp:1.11.0")
    implementation("org.atteo:evo-inflector:1.3")
    implementation("com.pinterest.ktlint:ktlint-rule-engine:1.4.1") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.4.1") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }

    // Logging dependency for ktlint.
    implementation("org.slf4j:slf4j-simple:2.0.3")

    testImplementation("dev.zacsweers.kctfork:core:0.6.0")
    testImplementation("dev.zacsweers.kctfork:ksp:0.6.0")
    testImplementation("com.h2database:h2:1.4.198")
}

if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
    tasks.test {
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }
}
