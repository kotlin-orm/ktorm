import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    // Lifecycle task for code generation.
    val codegen by registering { /* do nothing */ }

    compileJava {
        // Suppress warning for Java 8 deprecation.
        options.compilerArgs.add("-Xlint:-options")
    }

    compileKotlin {
        dependsOn(codegen)

        compilerOptions {
            allWarningsAsErrors.set(true)
            freeCompilerArgs.add("-Xexplicit-api=strict")
        }
    }

    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
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
