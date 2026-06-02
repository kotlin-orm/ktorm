
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    api("org.jetbrains.dokka:dokka-gradle-plugin:2.2.0")
    api("org.jetbrains.dokka:dokka-base:2.2.0")
    api("org.moditect:moditect:1.3.0.Final")
    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
}
