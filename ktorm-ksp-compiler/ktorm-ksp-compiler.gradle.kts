
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    implementation(project(":ktorm-core"))
    implementation(project(":ktorm-ksp-annotations"))
    implementation(project(":ktorm-ksp-spi"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.23-1.0.20")
    implementation("com.squareup:kotlinpoet-ksp:1.11.0")
    implementation("org.atteo:evo-inflector:1.3")
    implementation("com.pinterest.ktlint:ktlint-rule-engine:0.50.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:0.50.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }

    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.5.0")
    testImplementation("com.h2database:h2:1.4.198")
    testImplementation("org.slf4j:slf4j-simple:2.0.3")
}

if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
    tasks.test {
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }
}
