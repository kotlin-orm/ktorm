
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")
    api("com.squareup:kotlinpoet-ksp:1.11.0")
}
