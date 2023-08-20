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

package org.ktorm.ksp.compiler

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.squareup.kotlinpoet.FileSpec
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.Resource.Resources
import org.ktorm.ksp.annotation.Table
import org.ktorm.ksp.compiler.generator.FileGenerator
import org.ktorm.ksp.compiler.parser.MetadataParser
import org.ktorm.ksp.compiler.util.isValid
import org.ktorm.ksp.spi.TableMetadata
import java.util.*
import kotlin.reflect.jvm.jvmName

/**
 * Ktorm KSP symbol processor provider.
 */
public class KtormProcessorProvider : SymbolProcessorProvider {
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

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        for (generator in FileGenerator.extCodeGenerators) {
            environment.logger.info("[ktorm-ksp-compiler] load ext generator: $generator")
        }

        return object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                return doProcess(resolver, environment)
            }
        }
    }

    private fun doProcess(resolver: Resolver, environment: SymbolProcessorEnvironment): List<KSAnnotated> {
        val (symbols, deferral) = resolver.getSymbolsWithAnnotation(Table::class.jvmName).partition { it.isValid() }

        val parser = MetadataParser(resolver, environment)
        for (symbol in symbols) {
            if (symbol is KSClassDeclaration) {
                val table = parser.parseTableMetadata(symbol)
                generateFile(table, environment)
            }
        }

        return deferral
    }

    private fun generateFile(table: TableMetadata, environment: SymbolProcessorEnvironment) {
        // Generate file spec by kotlinpoet.
        val fileSpec = FileGenerator.generate(table, environment)

        // Beautify the generated code by ktlint.
        val formattedCode = formatCode(fileSpec, environment.logger)

        // Output the formatted code.
        val dependencies = Dependencies(false, *table.getDependencyFiles().toTypedArray())
        val file = environment.codeGenerator.createNewFile(dependencies, fileSpec.packageName, fileSpec.name)
        file.writer(Charsets.UTF_8).use { it.write(formattedCode) }
    }

    private fun formatCode(fileSpec: FileSpec, logger: KSPLogger): String {
        try {
            // Manually fix some code styles before formatting.
            val code = fileSpec.toString()
                .replace(Regex("""\(\s*"""), "(")
                .replace(Regex("""\s*\)"""), ")")
                .replace(Regex(""",\s*"""), ", ")
                .replace(Regex(""",\s*\)"""), ")")
                .replace(Regex("""\s+get\(\)\s="""), " get() =")
                .replace(Regex("""\s+=\s+"""), " = ")
                .replace("import org.ktorm.ksp.`annotation`", "import org.ktorm.ksp.annotation")

            return ktLintRuleEngine.format(Code.fromSnippet(code))
        } catch (e: Exception) {
            logger.exception(e)
            return fileSpec.toString()
        }
    }

    private fun TableMetadata.getDependencyFiles(): List<KSFile> {
        val files = ArrayList<KSFile>()

        val containingFile = entityClass.containingFile
        if (containingFile != null) {
            files += containingFile
        }

        for (column in columns) {
            val ref = column.referenceTable
            if (ref != null) {
                files += ref.getDependencyFiles()
            }
        }

        return files
    }
}
