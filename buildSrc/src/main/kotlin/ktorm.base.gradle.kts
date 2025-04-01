
group = rootProject.group
version = rootProject.version

plugins {
    id("kotlin")
    id("org.gradle.jacoco")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${detekt.toolVersion}")
}

detekt {
    source.setFrom("src/main/kotlin")
    config.setFrom("${project.rootDir}/detekt.yml")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    // Lifecycle task for code generation.
    val codegen by registering { /* do nothing */ }

    compileKotlin {
        dependsOn(codegen)

        kotlinOptions {
            jvmTarget = "11"
            allWarningsAsErrors = true
            freeCompilerArgs = listOf("-Xexplicit-api=strict")
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    jacocoTestReport {
        reports {
            csv.required.set(true)
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
