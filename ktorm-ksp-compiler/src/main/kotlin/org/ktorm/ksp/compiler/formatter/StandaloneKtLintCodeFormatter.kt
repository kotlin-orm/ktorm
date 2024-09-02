/*
 * Copyright 2018-2024 the original author or authors.
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
import java.util.concurrent.TimeUnit

internal class StandaloneKtLintCodeFormatter(val environment: SymbolProcessorEnvironment) : CodeFormatter {
    private val logger = environment.logger
    private val command = buildCommand()

    init {
        logger.info("[ktorm-ksp-compiler] init ktlint formatter with command: ${command.joinToString(" ")}")
    }

    override fun format(fileName: String, code: String): String {
        try {
            val p = ProcessBuilder(command).start()
            p.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(preprocessCode(code)) }

            if (!p.waitFor(30, TimeUnit.SECONDS)) {
                logger.info("[ktorm-ksp-compiler] ktlint execution timeout, skip code formatting for file: $fileName")
                return code
            }

            val formattedCode = p.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (p.exitValue() == 0) {
                // Exit normally.
                return formattedCode
            } else {
                if (formattedCode.isNotBlank()) {
                    // Some violations exist but the code is still formatted.
                    return formattedCode
                } else {
                    // Exit exceptionally.
                    val msg = p.errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    logger.error("[ktorm-ksp-compiler] ktlint exit with code: ${p.exitValue()}\n$msg")
                    return code
                }
            }
        } catch (e: Throwable) {
            logger.exception(e)
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
