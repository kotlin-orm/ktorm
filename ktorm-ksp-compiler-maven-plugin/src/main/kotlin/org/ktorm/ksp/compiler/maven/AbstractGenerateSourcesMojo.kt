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
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import org.ktorm.jackson.sharedObjectMapper
import org.ktorm.ksp.compiler.KtormProcessorProvider
import java.io.File

/**
 * Abstract maven plugin MOJO that handles Ktorm KSP code generation.
 */
public abstract class AbstractGenerateSourcesMojo : AbstractMojo() {

    protected abstract val project: MavenProject

    protected abstract val processorOptions: Map<String, String>

    protected abstract val sourceRoots: List<File>

    protected abstract val libraries: List<File>

    protected abstract val sourceOutputDirectory: File

    protected abstract val outputDirectory: File

    protected abstract val cachesDirectory: File

    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
        try {
            val config = buildKspConfig()
            if (log.isDebugEnabled) {
                log.debug("[ktorm-ksp-compiler] ksp config: ${sharedObjectMapper.writeValueAsString(config)}")
            }

            val code = KotlinSymbolProcessing(config, listOf(KtormProcessorProvider()), KspMavenLogger(log)).execute()
            if (code != ExitCode.OK) {
                throw MojoExecutionException("KSP failed with exit code: $code")
            }
        } catch (e: Throwable) {
            throw MojoFailureException("KSP failed with unexpected exception.", e)
        }
    }

    private fun buildKspConfig(): KSPJvmConfig {
        return KSPJvmConfig(
            javaSourceRoots = emptyList(),
            javaOutputDir = sourceOutputDirectory,
            jdkHome = jdkHome(),
            jvmTarget = jvmTarget(),
            jvmDefaultMode = "enable",
            moduleName = project.artifactId,
            sourceRoots = sourceRoots,
            commonSourceRoots = emptyList(),
            libraries = libraries,
            processorOptions = processorOptions,
            projectBaseDir = project.basedir,
            outputBaseDir = outputDirectory,
            cachesDir = cachesDirectory,
            classOutputDir = outputDirectory,
            kotlinOutputDir = sourceOutputDirectory,
            resourceOutputDir = outputDirectory,
            incremental = false,
            incrementalLog = false,
            modifiedSources = ArrayList(),
            removedSources = ArrayList(),
            changedClasses = ArrayList(),
            languageVersion = languageVersion(),
            apiVersion = apiVersion(),
            allWarningsAsErrors = allWarningsAsErrors(),
            mapAnnotationArgumentsInJava = false
        )
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
            ?: System.getProperty("java.version")
    }

    private fun languageVersion(): String {
        val version = kotlinPluginConfig("languageVersion")
            ?: project.properties.getProperty("kotlin.compiler.languageVersion")
            ?: project.properties.getProperty("kotlin.version")
            ?: project.build.pluginsAsMap["org.jetbrains.kotlin:kotlin-maven-plugin"]?.version
            ?: KotlinVersion.CURRENT.toString()

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
