package org.ktorm.ksp.compiler.formatter

/**
 * Code formatter interface.
 */
internal fun interface CodeFormatter {

    /**
     * Format the generated code to the community recommended coding style.
     */
    fun format(code: String): String
}
