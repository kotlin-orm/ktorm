
plugins {
    id("kotlin")
    id("org.moditect.gradleplugin")
}

moditect {
    // Generate a multi-release jar, the module descriptor will be located at META-INF/versions/9/module-info.class
    addMainModuleInfo {
        jvmVersion.set("9")
        overwriteExistingFiles.set(true)
        module {
            moduleInfoFile = file("src/main/moditect/module-info.java")
        }
    }

    // Let kotlin compiler know the module descriptor.
    if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
        sourceSets.main {
            kotlin.srcDir("src/main/moditect")
        }
    }

    // Workaround to avoid circular task dependencies, see https://github.com/moditect/moditect-gradle-plugin/issues/14
    afterEvaluate {
        val compileJava = tasks.compileJava.get()
        val addDependenciesModuleInfo = tasks.addDependenciesModuleInfo.get()
        compileJava.setDependsOn(compileJava.dependsOn - addDependenciesModuleInfo)
    }
}
