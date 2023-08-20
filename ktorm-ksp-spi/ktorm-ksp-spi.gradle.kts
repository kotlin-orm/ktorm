
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.13")
    api("com.squareup:kotlinpoet-ksp:1.11.0")
}
