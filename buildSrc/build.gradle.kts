
plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "2.1.7"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.32")
    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.20.0")
}
