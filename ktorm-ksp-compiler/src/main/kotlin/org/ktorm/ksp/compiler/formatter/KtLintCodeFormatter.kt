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
