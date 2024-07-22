import gradle.kotlin.dsl.accessors._f5007bc516eebc049270414be01b92ac.dokkaJavadoc

plugins {
    id("org.jetbrains.dokka")
}

tasks.named<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>("dokkaHtmlMultiModule") {
    val tmplDir = System.getenv("DOKKA_TEMPLATES_DIR")
    if (!tmplDir.isNullOrEmpty()) {
        pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
            templatesDir = File(tmplDir)
        }
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
