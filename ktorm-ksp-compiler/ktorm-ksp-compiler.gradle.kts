
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    implementation(project(":ktorm-core"))
    implementation(project(":ktorm-ksp-annotations"))
    implementation(project(":ktorm-ksp-spi"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.22-1.0.8")
    implementation("com.squareup:kotlinpoet-ksp:1.11.0")
    implementation("com.facebook:ktfmt:0.40")
    implementation("org.atteo:evo-inflector:1.3")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.8")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.8")
    testImplementation("com.h2database:h2:1.4.198")
    testImplementation("org.slf4j:slf4j-simple:1.7.25")
}
