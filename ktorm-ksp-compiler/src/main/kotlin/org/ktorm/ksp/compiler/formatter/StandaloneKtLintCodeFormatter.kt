/*
 * Copyright 2018-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.ksp.compiler.formatter

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import java.io.File

internal class StandaloneKtLintCodeFormatter(val environment: SymbolProcessorEnvironment) : CodeFormatter {
    private val command = buildCommand()

    init {
        environment.logger.info("[ktorm-ksp-compiler] init ktlint formatter with command: ${command.joinToString(" ")}")
    }

    override fun format(code: String): String {
        try {
            val p = ProcessBuilder(command).start()
            p.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(preprocessCode(code)) }
            p.waitFor()

            if (p.exitValue() == 0) {
                return p.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val msg = p.errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                environment.logger.error("[ktorm-ksp-compiler] ktlint exit with code: ${p.exitValue()}\n$msg")
                return code
            }
        } catch (e: Throwable) {
            environment.logger.exception(e)
            return code
        }
    }

    private fun buildCommand(): List<String> {
        val n = "java.lang.reflect.InaccessibleObjectException"
        val isJava8 = try { Class.forName(n); false } catch (_: ClassNotFoundException) { true }

        val java = findJavaExecutable()
        val ktlint = environment.options["ktorm.ktlintExecutable"]!!
        val config = createEditorConfigFile()

        if (isJava8) {
            return listOf(java, "-jar", ktlint, "-F", "--stdin", "--log-level=none", "--editorconfig=$config")
        } else {
            val jvmArgs = arrayOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
            return listOf(java, *jvmArgs, "-jar", ktlint, "-F", "--stdin", "--log-level=none", "--editorconfig=$config")
        }
    }

    private fun findJavaExecutable(): String {
        var file = File(File(System.getProperty("java.home"), "bin"), "java")
        if (!file.exists()) {
            file = File(File(System.getProperty("java.home"), "bin"), "java.exe")
        }

        if (file.exists()) {
            return file.path
        } else {
            throw IllegalStateException("Could not find java executable.")
        }
    }

    private fun createEditorConfigFile(): String {
        val file = File.createTempFile("ktlint", ".editorconfig")
        file.deleteOnExit()

        file.outputStream().use { output ->
            javaClass.classLoader.getResourceAsStream("ktorm-ksp-compiler/.editorconfig")!!.use { input ->
                input.copyTo(output)
            }
        }

        return file.path
    }

    private fun preprocessCode(code: String): String {
        return code
            .replace(Regex("""\(\s*"""), "(")
            .replace(Regex("""\s*\)"""), ")")
            .replace(Regex(""",\s*"""), ", ")
            .replace(Regex(""",\s*\)"""), ")")
            .replace(Regex("""\s+get\(\)\s="""), " get() =")
            .replace(Regex("""\s+=\s+"""), " = ")
            .replace("import org.ktorm.ksp.`annotation`", "import org.ktorm.ksp.annotation")
    }
}
