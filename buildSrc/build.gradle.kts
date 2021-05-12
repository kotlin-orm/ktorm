
plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "2.1.4"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.17.0-RC2")
}
