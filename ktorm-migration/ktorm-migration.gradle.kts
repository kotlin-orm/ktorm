
plugins {
    id("ktorm.module-conventions")
}

dependencies {
    api(project(":ktorm-core"))
    testImplementation("com.h2database:h2:1.4.197")
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
