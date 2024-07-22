
group = "org.ktorm"
version = file("ktorm.version").readLines()[0]

plugins {
    id("org.jetbrains.dokka") version "1.9.20"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.dokkaHtmlMultiModule {
    val templatesDir = System.getenv("DOKKA_TEMPLATES_DIR")
    if (!templatesDir.isNullOrEmpty()) {
        pluginsMapConfiguration.set(mapOf("org.jetbrains.dokka.base.DokkaBase" to """{"templatesDir": "$templatesDir"}"""))
    }
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")

    tasks.named<org.jetbrains.dokka.gradle.DokkaTaskPartial>("dokkaHtmlPartial") {
        dokkaSourceSets.named("main") {
            suppressGeneratedFiles.set(false)
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(java.net.URL("https://github.com/kotlin-orm/ktorm/blob/master/${project.name}/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
