
group = "org.ktorm"
version = file("ktorm.version").readLines()[0]

task("printClasspath") {
    doLast {
        val jars = subprojects
            .map { it.configurations["compileClasspath"] }
            .flatMap { it.files }
            .filterNotTo(HashSet()) { it.name.contains("ktorm") }
            .onEach { println(it.name) }

        val file = file("build/ktorm.classpath")
        file.parentFile.mkdirs()
        file.writeText(jars.joinToString(File.pathSeparator) { it.absolutePath })
        println("Classpath written to build/ktorm.classpath")
    }
}
