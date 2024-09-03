
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    api("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
    api("org.jetbrains.dokka:dokka-base:1.9.20")
    api("org.moditect:moditect:1.0.0.RC1")
    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.6")
}
