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

    protected abstract val kotlinCompileMojoConfiguration: Xpp3Dom?

    @Throws(MojoExecutionException::class, MojoFailureException::class)
    protected fun generateSources(): List<File> {
        try {
            val kotlinCompileMojoConfiguration = this.kotlinCompileMojoConfiguration
            val sourceDirs = sourceDirs(kotlinCompileMojoConfiguration)

            val sourceRoots = (this.sourceRoots + sourceDirs).distinct()
            val libraries = this.libraries
            val sourceOutputDirectory = this.sourceOutputDirectory
            val outputDirectory = this.outputDirectory

            val config = KSPJvmConfig(
                javaSourceRoots = sourceRoots,
                javaOutputDir = sourceOutputDirectory,
                jdkHome = jdkHome(kotlinCompileMojoConfiguration),
                jvmTarget = jvmTarget(kotlinCompileMojoConfiguration),
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
                languageVersion = languageVersion(kotlinCompileMojoConfiguration),
                apiVersion = apiVersion(kotlinCompileMojoConfiguration),
                allWarningsAsErrors = allWarningsAsErrors(kotlinCompileMojoConfiguration),
                mapAnnotationArgumentsInJava = false
            )

            if (log.isDebugEnabled) {
                log.debug("[ktorm-ksp-compiler] ksp config: ${sharedObjectMapper.writeValueAsString(config)}")
            }

            val code = KotlinSymbolProcessing(config, listOf(KtormProcessorProvider()), KspMavenLogger(log)).execute()
            if (code != ExitCode.OK) {
                throw MojoExecutionException("KSP failed with exit code: $code")
            }

            return (sourceDirs + sourceOutputDirectory).distinct()
        } catch (e: Throwable) {
            throw MojoFailureException("KSP failed with unexpected exception.", e)
        }
    }

    private fun sourceDirs(configuration: Xpp3Dom?): List<File> {
        return configuration?.getChild("sourceDirs")?.getChildren("sourceDir")
            ?.map { File(it.value) }
            ?.map { if (it.isAbsolute) it else File(project.basedir, it.path) }
            ?: emptyList()
    }

    private fun jdkHome(configuration: Xpp3Dom?): File {
        val jdkHome = configuration?.getChildValue("jdkHome")
            ?: project.properties.getProperty("kotlin.compiler.jdkHome")
            ?: System.getProperty("java.home")

        return File(jdkHome)
    }

    private fun jvmTarget(configuration: Xpp3Dom?): String {
        return configuration?.getChildValue("jvmTarget")
            ?: project.properties.getProperty("kotlin.compiler.jvmTarget")
            ?: project.properties.getProperty("maven.compiler.release")
            ?: project.properties.getProperty("maven.compiler.target")
            ?: System.getProperty("java.version")
    }

    private fun languageVersion(configuration: Xpp3Dom?): String {
        val version = configuration?.getChildValue("languageVersion")
            ?: project.properties.getProperty("kotlin.compiler.languageVersion")
            ?: project.properties.getProperty("kotlin.version")
            ?: project.build.pluginsAsMap["org.jetbrains.kotlin:kotlin-maven-plugin"]?.version
            ?: KotlinVersion.CURRENT.toString()

        val arr = version.split(".")
        return if (arr.size >= 2) "${arr[0]}.${arr[1]}" else version
    }

    private fun apiVersion(configuration: Xpp3Dom?): String {
        val version = configuration?.getChildValue("apiVersion")
            ?: project.properties.getProperty("kotlin.compiler.apiVersion")
            ?: languageVersion(configuration)

        val arr = version.split(".")
        return if (arr.size >= 2) "${arr[0]}.${arr[1]}" else version
    }

    private fun allWarningsAsErrors(configuration: Xpp3Dom?): Boolean {
        val flag = configuration?.getChildValue("allWarningsAsErrors")
            ?: project.properties.getProperty("kotlin.compiler.allWarningsAsErrors")

        return flag?.toBoolean() == true
    }

    private fun Xpp3Dom.getChildValue(name: String): String? {
        val child = getChild(name)
        return child?.value
    }
}
