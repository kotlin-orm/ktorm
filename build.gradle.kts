
group = "org.ktorm"
version = "3.5.0-SNAPSHOT"

task("printClasspath") {
    doLast {
        val jars = subprojects.flatMapTo(HashSet()) { it.configurations["compileClasspath"].files }
        jars.removeIf { it.name.contains("ktorm") }

        println("Project classpath: ")
        jars.forEach { println(it.name) }

        val file = file("build/ktorm.classpath")
        file.parentFile.mkdirs()
        file.writeText(jars.joinToString(File.pathSeparator) { it.absolutePath })
        println("Classpath written to build/ktorm.classpath")
    }
}
