
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    api("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    api("org.jetbrains.dokka:dokka-base:2.0.0")
    api("org.moditect:moditect:1.0.0.RC1")
    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.6")
}
