
plugins {
    id("ktorm.base")
    id("ktorm.publish")
    id("ktorm.source-header-check")
}

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:2.3.7")
    api("com.squareup:kotlinpoet-ksp:2.3.0")
}
