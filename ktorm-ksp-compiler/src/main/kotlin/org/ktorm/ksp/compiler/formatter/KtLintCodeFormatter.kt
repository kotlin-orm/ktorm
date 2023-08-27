package org.ktorm.ksp.compiler.formatter

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.Resource.Resources
import java.util.*

internal class KtLintCodeFormatter(val environment: SymbolProcessorEnvironment) : CodeFormatter {
    private val ktLintRuleEngine = KtLintRuleEngine(
        ruleProviders = ServiceLoader
            .load(RuleSetProviderV3::class.java, javaClass.classLoader)
            .flatMap { it.getRuleProviders() }
            .toSet(),
        editorConfigDefaults = EditorConfigDefaults(
            EditorConfigLoader.default_().load(
                Resources.ofClassPath(javaClass.classLoader, "/ktorm-ksp-compiler/.editorconfig", Charsets.UTF_8)
            )
        )
    )

    override fun format(code: String): String {
        try {
            // Manually fix some code styles before formatting.
            val snippet = code
                .replace(Regex("""\(\s*"""), "(")
                .replace(Regex("""\s*\)"""), ")")
                .replace(Regex(""",\s*"""), ", ")
                .replace(Regex(""",\s*\)"""), ")")
                .replace(Regex("""\s+get\(\)\s="""), " get() =")
                .replace(Regex("""\s+=\s+"""), " = ")
                .replace("import org.ktorm.ksp.`annotation`", "import org.ktorm.ksp.annotation")

            return ktLintRuleEngine.format(Code.fromSnippet(snippet))
        } catch (e: Throwable) {
            environment.logger.exception(e)
            return code
        }
    }
}
