package org.ktorm.ksp.compiler.formatter

internal class SimpleCodeFormatter : CodeFormatter {

    override fun format(fileName: String, code: String): String {
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
