
plugins {
    id("kotlin")
}

val moditect by tasks.registering {
    doLast {
        // Generate a multi-release modulized jar, module descriptor position: META-INF/versions/9/module-info.class
        val inputJar = tasks.jar.flatMap { it.archiveFile }.map { it.asFile.toPath() }.get()
        val outputDir = file("build/moditect").apply { mkdirs() }.toPath()
        val moduleInfo = file("src/main/moditect/module-info.java").readText()
        val version = project.version.toString()
        org.moditect.commands.AddModuleInfo(moduleInfo, null, version, inputJar, outputDir, "9", true).run()

        // Replace the original jar with the modulized jar.
        copy {
            from(outputDir.resolve(inputJar.fileName))
            into(inputJar.parent)
        }
    }
}

tasks {
    moditect {
        dependsOn(jar)
    }
    jar {
        finalizedBy(moditect)
    }
}

if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
    // Let kotlin compiler know the module descriptor.
    sourceSets.main {
        kotlin.srcDir("src/main/moditect")
    }
}
