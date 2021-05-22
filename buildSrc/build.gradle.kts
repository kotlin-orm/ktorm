
plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "2.1.4"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.0") {
        // TODO: Remove the exclusions when this issue fixed: https://youtrack.jetbrains.com/issue/KT-41142
        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }

    api("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.17.0-RC2")
}
