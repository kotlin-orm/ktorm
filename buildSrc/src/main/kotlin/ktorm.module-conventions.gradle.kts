
group = rootProject.group
version = rootProject.version

plugins {
    id("kotlin")
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

detekt {
    source = files("src/main/kotlin")
    config = files("${project.rootDir}/detekt.yml")
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = true
        freeCompilerArgs = listOf(
            "-Xexplicit-api=strict",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-Xjvm-default=enable"
        )
    }
}
