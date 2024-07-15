
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    api("org.moditect:moditect-gradle-plugin:1.0.0-rc3")
    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.6")
}
