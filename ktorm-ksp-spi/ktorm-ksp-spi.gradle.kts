
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:1.7.22-1.0.8")
    api("com.squareup:kotlinpoet-ksp:1.11.0")
}
