package org.ktorm.ksp.compiler

import com.tschuchort.compiletesting.*
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Before
import org.ktorm.database.Database
import org.ktorm.database.use
import java.lang.reflect.InvocationTargetException

abstract class BaseKspTest {
    lateinit var database: Database

    @Before
    fun init() {
        database = Database.connect("jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1", alwaysQuoteIdentifiers = true)
        execSqlScript("init-ksp-data.sql")
    }

    @After
    fun destroy() {
        execSqlScript("drop-ksp-data.sql")
    }

    private fun execSqlScript(filename: String) {
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                javaClass.classLoader
                    ?.getResourceAsStream(filename)
                    ?.bufferedReader()
                    ?.use { reader ->
                        for (sql in reader.readText().split(';')) {
                            if (sql.any { it.isLetterOrDigit() }) {
                                statement.executeUpdate(sql)
                            }
                        }
                    }
            }
        }
    }

    protected fun kspFailing(message: String, @Language("kotlin") code: String, vararg options: Pair<String, String>) {
        val result = compile(code, emptyList(), mapOf(*options))
        assert(result.exitCode == KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assert(result.messages.contains("e: Error occurred in KSP, check log for detail"))
        assert(result.messages.contains(message))
    }

    protected fun runKotlin(@Language("kotlin") code: String, additionalImports: List<String> = emptyList(), vararg options: Pair<String, String>) {
        val result = compile(code, additionalImports, mapOf(*options))
        assert(result.exitCode == KotlinCompilation.ExitCode.OK)

        try {
            val cls = result.classLoader.loadClass("SourceKt")
            cls.getMethod("setDatabase", Database::class.java).invoke(null, database)
            cls.getMethod("run").invoke(null)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun compile(@Language("kotlin") code: String, additionalImports: List<String>, options: Map<String, String>): KotlinCompilation.Result {
        @Language("kotlin")
        val header = """
            import java.math.*
            import java.sql.*
            import java.time.*
            import java.util.*
            import kotlin.reflect.*
            import kotlin.reflect.jvm.*
            import org.ktorm.database.*
            import org.ktorm.dsl.*
            import org.ktorm.entity.*
            import org.ktorm.ksp.annotation.*

            ${additionalImports.joinToString("\n") { "import $it" }}
            
            lateinit var database: Database
            
            
        """.trimIndent()

        val source = header + code
        printFile(source, "Source.kt")

        val compilation = createCompilation(SourceFile.kotlin("Source.kt", source), options)
        val result = compilation.compile()

        for (file in compilation.kspSourcesDir.walk()) {
            if (file.isFile) {
                printFile(file.readText(), "Generated file: ${file.absolutePath}")
            }
        }

        return result
    }

    private fun createCompilation(source: SourceFile, options: Map<String, String>): KotlinCompilation {
        return KotlinCompilation().apply {
            sources = listOf(source)
            verbose = false
            messageOutputStream = System.out
            inheritClassPath = true
            allWarningsAsErrors = true
            symbolProcessorProviders = listOf(KtormProcessorProvider())
            kspIncremental = true
            kspWithCompilation = true
            kspArgs += options
        }
    }

    private fun printFile(text: String, title: String) {
        val lines = text.lines()
        val gutterSize = lines.size.toString().count()

        println("${"#".repeat(gutterSize + 2)}-----------------------------------------")
        println("${"#".repeat(gutterSize + 2)} $title")
        println("${"#".repeat(gutterSize + 2)}-----------------------------------------")

        for ((i, line) in lines.withIndex()) {
            println(String.format("#%${gutterSize}d| %s", i + 1, line))
        }
    }
}
