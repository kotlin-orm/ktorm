
group = rootProject.group
version = rootProject.version

plugins {
    id("kotlin")
    id("org.moditect.gradleplugin")
    id("io.gitlab.arturbosch.detekt")
    id("ktorm.source-header-check")
    id("ktorm.maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${detekt.toolVersion}")
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
    if (JavaVersion.current() >= JavaVersion.VERSION_1_9 && System.getenv("CI") == "true") {
        kotlin {
            sourceSets.main {
                kotlin.srcDir("src/main/moditect")
            }
        }
    }

    // Workaround to avoid circular task dependencies, see https://github.com/moditect/moditect-gradle-plugin/issues/14
    afterEvaluate {
        val compileJava = tasks.compileJava.get()
        val addDependenciesModuleInfo = tasks.addDependenciesModuleInfo.get()
        compileJava.setDependsOn(compileJava.dependsOn - addDependenciesModuleInfo)
    }
}

detekt {
    source = files("src/main/kotlin")
    config = files("${project.rootDir}/detekt.yml")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            allWarningsAsErrors = true
            freeCompilerArgs = listOf(
                "-Xexplicit-api=strict",
                "-Xopt-in=kotlin.RequiresOptIn"
            )
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf(
                "-Xjvm-default=enable"
            )
        }
    }
}
