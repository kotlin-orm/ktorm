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

package org.ktorm.ksp.compiler.maven

import com.google.devtools.ksp.KspCliOption
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.project.MavenProject
import org.apache.maven.repository.RepositorySystem
import org.codehaus.plexus.component.annotations.Component
import org.codehaus.plexus.component.annotations.Requirement
import org.jetbrains.kotlin.maven.KotlinMavenPluginExtension
import org.jetbrains.kotlin.maven.PluginOption
import java.io.File

/**
 * Extension that enables KSP for the kotlin maven plugin.
 */
@Component(role = KotlinMavenPluginExtension::class, hint = "ksp")
public class KtormKspMavenPluginExtension : KotlinMavenPluginExtension {
    @Requirement
    private lateinit var repositorySystem: RepositorySystem
    @Requirement
    private lateinit var mavenSession: MavenSession

    override fun getCompilerPluginId(): String {
        return "com.google.devtools.ksp.symbol-processing"
    }

    override fun isApplicable(project: MavenProject, execution: MojoExecution): Boolean {
        return execution.mojoDescriptor.goal == "compile" || execution.mojoDescriptor.goal == "test-compile"
    }

    override fun getPluginOptions(project: MavenProject, execution: MojoExecution): List<PluginOption> {
        val userOptions = parseUserOptions(execution)

        if (execution.mojoDescriptor.goal == "compile") {
            val options = buildPluginOptions(project, execution, userOptions)
            for (key in listOf(KspCliOption.JAVA_OUTPUT_DIR_OPTION, KspCliOption.KOTLIN_OUTPUT_DIR_OPTION)) {
                project.addCompileSourceRoot(options[key] ?: userOptions[key]!![0])
            }

            return options.map { (option, value) -> PluginOption("ksp", compilerPluginId, option.optionName, value) }
        }

        if (execution.mojoDescriptor.goal == "test-compile") {
            val options = buildTestPluginOptions(project, execution, userOptions)
            for (key in listOf(KspCliOption.JAVA_OUTPUT_DIR_OPTION, KspCliOption.KOTLIN_OUTPUT_DIR_OPTION)) {
                project.addTestCompileSourceRoot(options[key] ?: userOptions[key]!![0])
            }

            return options.map { (option, value) -> PluginOption("ksp", compilerPluginId, option.optionName, value) }
        }

        return emptyList()
    }

    private fun parseUserOptions(execution: MojoExecution): Map<KspCliOption, List<String>> {
        val pluginOptions = execution.configuration.getChild("pluginOptions") ?: return emptyMap()
        val availableOptions = KspCliOption.entries.associateBy { it.optionName }
        val pattern = Regex("([^:]+):([^=]+)=(.*)")

        return pluginOptions.children
            .mapNotNull { pattern.matchEntire(it.value) }
            .map { it.destructured }
            .filter { (plugin, key, value) -> plugin == "ksp" && key in availableOptions && value.isNotBlank() }
            .groupBy({ (_, key, _) -> availableOptions[key]!! }, { (_, _, value) -> value })
    }

    private fun buildPluginOptions(
        project: MavenProject, execution: MojoExecution, userOptions: Map<KspCliOption, List<String>>
    ): Map<KspCliOption, String> {
        val baseDir = project.basedir.path
        val buildDir = project.build.directory
        val options = LinkedHashMap<KspCliOption, String>()

        if (KspCliOption.CLASS_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.CLASS_OUTPUT_DIR_OPTION] = project.build.outputDirectory
        }
        if (KspCliOption.JAVA_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.JAVA_OUTPUT_DIR_OPTION] = path(buildDir, "generated-sources", "ksp-java")
        }
        if (KspCliOption.KOTLIN_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.KOTLIN_OUTPUT_DIR_OPTION] = path(buildDir, "generated-sources", "ksp")
        }
        if (KspCliOption.RESOURCE_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.RESOURCE_OUTPUT_DIR_OPTION] = project.build.outputDirectory
        }
        if (KspCliOption.CACHES_DIR_OPTION !in userOptions) {
            options[KspCliOption.CACHES_DIR_OPTION] = path(buildDir, "ksp-caches")
        }
        if (KspCliOption.PROJECT_BASE_DIR_OPTION !in userOptions) {
            options[KspCliOption.PROJECT_BASE_DIR_OPTION] = baseDir
        }
        if (KspCliOption.KSP_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.KSP_OUTPUT_DIR_OPTION] = path(buildDir, "ksp")
        }
        if (KspCliOption.PROCESSOR_CLASSPATH_OPTION !in userOptions) {
            options[KspCliOption.PROCESSOR_CLASSPATH_OPTION] = processorClasspath(project, execution)
        }
        if (KspCliOption.WITH_COMPILATION_OPTION !in userOptions) {
            options[KspCliOption.WITH_COMPILATION_OPTION] = "true"
        }

        val apOptions = userOptions[KspCliOption.PROCESSING_OPTIONS_OPTION] ?: emptyList()
        if (apOptions.none { it.startsWith("ktorm.ktlintExecutable=") }) {
            options[KspCliOption.PROCESSING_OPTIONS_OPTION] = "ktorm.ktlintExecutable=${ktlintExecutable(project)}"
        }

        return options
    }

    private fun buildTestPluginOptions(
        project: MavenProject, execution: MojoExecution, userOptions: Map<KspCliOption, List<String>>
    ): Map<KspCliOption, String> {
        val baseDir = project.basedir.path
        val buildDir = project.build.directory
        val options = LinkedHashMap<KspCliOption, String>()

        if (KspCliOption.CLASS_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.CLASS_OUTPUT_DIR_OPTION] = project.build.testOutputDirectory
        }
        if (KspCliOption.JAVA_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.JAVA_OUTPUT_DIR_OPTION] = path(buildDir, "generated-test-sources", "ksp-java")
        }
        if (KspCliOption.KOTLIN_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.KOTLIN_OUTPUT_DIR_OPTION] = path(buildDir, "generated-test-sources", "ksp")
        }
        if (KspCliOption.RESOURCE_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.RESOURCE_OUTPUT_DIR_OPTION] = project.build.testOutputDirectory
        }
        if (KspCliOption.CACHES_DIR_OPTION !in userOptions) {
            options[KspCliOption.CACHES_DIR_OPTION] = path(buildDir, "ksp-caches")
        }
        if (KspCliOption.PROJECT_BASE_DIR_OPTION !in userOptions) {
            options[KspCliOption.PROJECT_BASE_DIR_OPTION] = baseDir
        }
        if (KspCliOption.KSP_OUTPUT_DIR_OPTION !in userOptions) {
            options[KspCliOption.KSP_OUTPUT_DIR_OPTION] = path(buildDir, "ksp-test")
        }
        if (KspCliOption.PROCESSOR_CLASSPATH_OPTION !in userOptions) {
            options[KspCliOption.PROCESSOR_CLASSPATH_OPTION] = processorClasspath(project, execution)
        }
        if (KspCliOption.WITH_COMPILATION_OPTION !in userOptions) {
            options[KspCliOption.WITH_COMPILATION_OPTION] = "true"
        }

        val apOptions = userOptions[KspCliOption.PROCESSING_OPTIONS_OPTION] ?: emptyList()
        if (apOptions.none { it.startsWith("ktorm.ktlintExecutable=") }) {
            options[KspCliOption.PROCESSING_OPTIONS_OPTION] = "ktorm.ktlintExecutable=${ktlintExecutable(project)}"
        }

        return options
    }

    private fun processorClasspath(project: MavenProject, execution: MojoExecution): String {
        val files = ArrayList<File>()
        for (dependency in execution.plugin.dependencies) {
            val r = ArtifactResolutionRequest()
            r.artifact = repositorySystem.createDependencyArtifact(dependency)
            r.localRepository = mavenSession.localRepository
            r.remoteRepositories = project.pluginArtifactRepositories
            r.isResolveTransitively = true

            val resolved = repositorySystem.resolve(r)
            files += resolved.artifacts.mapNotNull { it.file }.filter { it.exists() }
        }

        return files.joinToString(File.pathSeparator) { it.path }
    }

    private fun ktlintExecutable(project: MavenProject): String {
        val r = ArtifactResolutionRequest()
        r.artifact = repositorySystem.createArtifactWithClassifier("com.pinterest", "ktlint", "0.50.0", "jar", "all")
        r.localRepository = mavenSession.localRepository
        r.remoteRepositories = project.pluginArtifactRepositories
        r.isResolveTransitively = false

        val resolved = repositorySystem.resolve(r)
        val file = resolved.artifacts.mapNotNull { it.file }.firstOrNull { it.exists() }
        if (file != null) {
            return file.path
        } else {
            throw MojoExecutionException("Resolve ktlint executable jar failed.")
        }
    }

    private fun path(parent: String, vararg children: String): String {
        val file = children.fold(File(parent)) { acc, child -> File(acc, child) }
        return file.path
    }
}
