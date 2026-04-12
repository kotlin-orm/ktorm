/*
 * Copyright 2018-2026 the original author or authors.
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

package org.ktorm.ksp.compiler.maven

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.impl.KotlinSymbolProcessing.ExitCode
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import org.ktorm.ksp.compiler.KtormProcessorProvider
import java.io.File

/**
 * Maven plugin MOJO that handles Ktorm KSP code generation.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateSourcesMojo : AbstractMojo() {
    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    @Parameter
    private lateinit var processorOptions: Map<String, String>

    override fun execute() {
        val config = buildKspConfig()

        try {
            val code = KotlinSymbolProcessing(config, listOf(KtormProcessorProvider()), log.toKspLogger()).execute()
            if (code != ExitCode.OK) {
                throw MojoFailureException("KSP failed with exit code: $code")
            }
        } finally {
            project.addCompileSourceRoot(config.kotlinOutputDir.path)
        }
    }

    private fun buildKspConfig(): KSPJvmConfig {
        val buildDir = File(project.build.directory)
        val outputDir = File(project.build.outputDirectory)
        val kspSrcDir = File(buildDir, "generated-sources", "ksp")
        val kspCachesDir = File(buildDir, "ksp-caches")

        return KSPJvmConfig(
            javaSourceRoots = emptyList(),
            javaOutputDir = kspSrcDir,
            jdkHome = jdkHome(),
            jvmTarget = jvmTarget(),
            jvmDefaultMode = "enable",
            moduleName = project.artifactId,
            sourceRoots = project.compileSourceRoots.map { File(it) },
            commonSourceRoots = emptyList(),
            libraries = project.compileClasspathElements.map { File(it) },
            processorOptions = processorOptions,
            projectBaseDir = project.basedir,
            outputBaseDir = File(project.build.outputDirectory),
            cachesDir = kspCachesDir,
            classOutputDir = outputDir,
            kotlinOutputDir = kspSrcDir,
            resourceOutputDir = outputDir,
            incremental = true,
            incrementalLog = true,
            modifiedSources = emptyList(),
            removedSources = emptyList(),
            changedClasses = emptyList(),
            languageVersion = languageVersion(),
            apiVersion = apiVersion(),
            allWarningsAsErrors = allWarningsAsErrors(),
            mapAnnotationArgumentsInJava = false
        )
    }

    private fun Log.toKspLogger() = object : KSPLogger {
        private val log = this@toKspLogger

        override fun logging(message: String, symbol: KSNode?) {
            log.debug(format(message, symbol))
        }

        override fun info(message: String, symbol: KSNode?) {
            log.info(format(message, symbol))
        }

        override fun warn(message: String, symbol: KSNode?) {
            log.warn(format(message, symbol))
        }

        override fun error(message: String, symbol: KSNode?) {
            log.error(format(message, symbol))
        }

        override fun exception(e: Throwable) {
            log.error("[ktorm-ksp-compiler] ${e.message}", e)
        }

        private fun format(message: String, symbol: KSNode?) =
            when (val location = symbol?.location) {
                is FileLocation -> "[ktorm-ksp-compiler] ${location.filePath}:${location.lineNumber}: $message"
                is NonExistLocation, null -> "[ktorm-ksp-compiler] $message"
            }
    }

    private fun File(parent: File, vararg children: String): File {
        return children.fold(parent) { acc, child -> java.io.File(acc, child) }
    }

    private fun kotlinPluginConfig(name: String): String? {
        val plugin = project.build.pluginsAsMap["org.jetbrains.kotlin:kotlin-maven-plugin"]
        val config = (plugin?.configuration as? Xpp3Dom)?.getChild(name)
        return config?.value
    }

    private fun jdkHome(): File {
        val jdkHome = kotlinPluginConfig("jdkHome")
            ?: project.properties.getProperty("kotlin.compiler.jdkHome")
            ?: System.getProperty("java.home")

        return File(jdkHome)
    }

    private fun jvmTarget(): String {
        return kotlinPluginConfig("jvmTarget")
            ?: project.properties.getProperty("kotlin.compiler.jvmTarget")
            ?: project.properties.getProperty("maven.compiler.release")
            ?: project.properties.getProperty("maven.compiler.target")
            ?: "1.8"
    }

    private fun languageVersion(): String {
        val version = kotlinPluginConfig("languageVersion")
            ?: project.properties.getProperty("kotlin.compiler.languageVersion")
            ?: project.properties.getProperty("kotlin.version")
            ?: "2.0.21"

        val arr = version.split(".")
        return if (arr.size >= 2) "${arr[0]}.${arr[1]}" else version
    }
    
    private fun apiVersion(): String {
        val version = kotlinPluginConfig("apiVersion")
            ?: project.properties.getProperty("kotlin.compiler.apiVersion")
            ?: languageVersion()

        val arr = version.split(".")
        return if (arr.size >= 2) "${arr[0]}.${arr[1]}" else version
    }

    private fun allWarningsAsErrors(): Boolean {
        val flag = kotlinPluginConfig("allWarningsAsErrors")
            ?: project.properties.getProperty("kotlin.compiler.allWarningsAsErrors")

        return flag?.toBoolean() == true
    }
}
