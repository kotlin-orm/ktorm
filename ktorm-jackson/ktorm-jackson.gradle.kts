
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))
    api("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.3")
    compileOnly("org.postgresql:postgresql:42.2.5")
}

val generatedSourceDir = "${project.buildDir.absolutePath}/generated/source/main/kotlin"

val generatePackageVersion by tasks.registering(Copy::class) {
    from("src/main/kotlin/org/ktorm/jackson/PackageVersion.kt.tmpl")
    into("${generatedSourceDir}/org/ktorm/jackson")

    rename("(.+)\\.tmpl", "$1")
    expand(project.properties)
}

sourceSets.main {
    java.srcDirs(generatedSourceDir)
}

tasks["compileKotlin"].dependsOn(generatePackageVersion)
tasks["jarSources"].dependsOn(generatePackageVersion)
