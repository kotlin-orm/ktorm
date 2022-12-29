
plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "2.4.1"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.22")
    api("org.moditect:moditect-gradle-plugin:1.0.0-rc3")
    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.20.0")
}
