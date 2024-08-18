
plugins {
    id("org.jetbrains.dokka")
}

tasks.named<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>("dokkaHtmlMultiModule") {
    val tmplDir = System.getProperty("dokka.templatesDir")
    if (!tmplDir.isNullOrEmpty()) {
        pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
            templatesDir = File(tmplDir)
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")

    tasks.dokkaJavadoc {
        dependsOn("codegen")

        dokkaSourceSets.named("main") {
            suppressGeneratedFiles.set(false)
        }
    }

    tasks.named<org.jetbrains.dokka.gradle.DokkaTaskPartial>("dokkaHtmlPartial") {
        dependsOn("codegen")

        val tmplDir = System.getProperty("dokka.templatesDir")
        if (!tmplDir.isNullOrEmpty()) {
            pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
                templatesDir = File(tmplDir)
            }
        }

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
