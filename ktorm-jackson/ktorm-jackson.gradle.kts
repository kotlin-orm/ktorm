
plugins {
    id("ktorm.module")
}

dependencies {
    api(project(":ktorm-core"))
    api("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.3")
}

val generatedSourceDir = "${project.buildDir.absolutePath}/generated/source/main/kotlin"

val generatePackageVersion by tasks.registering(Copy::class) {
    from("src/main/kotlin/org/ktorm/jackson/PackageVersion.kt.tmpl")
    into("${generatedSourceDir}/org/ktorm/jackson")
    rename("(.+)\\.tmpl", "$1")
    expand(project.properties)
}

tasks {
    compileKotlin {
        dependsOn(generatePackageVersion)
    }
    "jarSources" {
        dependsOn(generatePackageVersion)
    }
}

sourceSets.main {
    kotlin.srcDir(generatedSourceDir)
}
