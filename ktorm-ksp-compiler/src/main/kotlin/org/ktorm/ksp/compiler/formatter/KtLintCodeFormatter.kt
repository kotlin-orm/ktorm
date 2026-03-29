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
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import java.util.*

internal class KtLintCodeFormatter(val environment: SymbolProcessorEnvironment) : CodeFormatter() {
    private val ktLintRuleEngine = initRuleEngine()

    private fun initRuleEngine(): KtLintRuleEngine? {
        try {
            val ruleProviders = ServiceLoader
                .load(RuleSetProviderV3::class.java, javaClass.classLoader)
                .flatMap { it.getRuleProviders() }
                .toSet()

            val configFile = createEditorConfigFile()
            val editorConfig = EditorConfigDefaults.load(configFile.toPath(), ruleProviders.propertyTypes())

            return KtLintRuleEngine(
                ruleProviders = ruleProviders,
                editorConfigDefaults = editorConfig
            )
        } catch (e: Throwable) {
            environment.logger.exception(e)
            return null
        }
    }

    override fun format(fileName: String, code: String): String {
        if (ktLintRuleEngine == null) {
            return code
        }

        try {
            var snippet = Code.fromSnippet(preformat(code))
            return ktLintRuleEngine.format(snippet) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }
        } catch (e: Throwable) {
            environment.logger.exception(e)
            return code
        }
    }
}
