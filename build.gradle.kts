
group = "org.ktorm"
version = file("ktorm.version").readLines()[0]

plugins {
    id("ktorm.dokka")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
