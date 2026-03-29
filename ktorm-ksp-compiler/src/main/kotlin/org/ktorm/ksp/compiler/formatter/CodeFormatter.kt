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

import java.io.File

/**
 * Code formatter.
 */
internal abstract class CodeFormatter {

    /**
     * Format the generated code to the community recommended coding style.
     */
    abstract fun format(fileName: String, code: String): String

    /**
     * Manually fix some style issues before formatting.
     */
    protected fun preformat(code: String): String {
        return code
            .replace(Regex("""\(\s*"""), "(")
            .replace(Regex("""\s*\)"""), ")")
            .replace(Regex(""",\s*"""), ", ")
            .replace(Regex(""",\s*\)"""), ")")
            .replace(Regex("""\s+get\(\)\s="""), " get() =")
            .replace(Regex("""\s+=\s+"""), " = ")
            .replace("import org.ktorm.ksp.`annotation`", "import org.ktorm.ksp.annotation")
    }

    /**
     * Create a temp editor config file.
     */
    protected fun createEditorConfigFile(): File {
        val file = File.createTempFile("ktlint", ".editorconfig")
        file.deleteOnExit()

        file.outputStream().use { output ->
            javaClass.classLoader.getResourceAsStream("ktorm-ksp-compiler/.editorconfig")!!.use { input ->
                input.copyTo(output)
            }
        }

        return file
    }
}
