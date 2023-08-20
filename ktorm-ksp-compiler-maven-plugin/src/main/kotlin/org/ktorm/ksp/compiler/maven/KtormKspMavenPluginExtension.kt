package org.ktorm.ksp.compiler.maven

import com.google.devtools.ksp.KspCliOption
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.apache.maven.repository.RepositorySystem
import org.codehaus.plexus.component.annotations.Component
import org.codehaus.plexus.component.annotations.Requirement
import org.jetbrains.kotlin.maven.KotlinMavenPluginExtension
import org.jetbrains.kotlin.maven.PluginOption
import java.io.File

/**
 * Extension that enables KSP for the kotlin maven plugin
 */
@Component(role = KotlinMavenPluginExtension::class, hint = "ksp")
public class KtormKspMavenPluginExtension : KotlinMavenPluginExtension {
    @Requirement
    private lateinit var repositorySystem: RepositorySystem

    override fun getCompilerPluginId(): String {
        return "com.google.devtools.ksp.symbol-processing"
    }

    override fun isApplicable(project: MavenProject, execution: MojoExecution): Boolean {
        return execution.mojoDescriptor.goal == "compile" || execution.mojoDescriptor.goal == "test-compile"
    }

    override fun getPluginOptions(project: MavenProject, execution: MojoExecution): List<PluginOption> {
//        val request = ArtifactResolutionRequest()
//        request.artifact = repositorySystem.createArtifactWithClassifier("com.pinterest", "ktlint", "0.50.0", "jar", "all")
//        request.remoteRepositories = project.remoteArtifactRepositories
//
//        val resolved = repositorySystem.resolve(request)
//        throw MojoExecutionException("test ktorm ksp: ${resolved.artifacts.map { it.file }}")
        val options = buildDefaultOptions(project, execution)
        return options.map { (option, value) -> PluginOption("ksp", compilerPluginId, option.optionName, value) }
    }

    private fun parseUserOptions(execution: MojoExecution): Map<KspCliOption, List<String>> {
        val pluginOptions = execution.configuration.getChild("pluginOptions") ?: return emptyMap()
        val availableOptions = KspCliOption.values().associateBy { it.optionName }
        val pattern = Regex("([^:]+):([^=]+)=(.*)")

        return pluginOptions.children
            .mapNotNull { pattern.matchEntire(it.value) }
            .map { it.destructured }
            .filter { (plugin, key, _) -> plugin == "ksp" && key in availableOptions }
            .groupBy({ (_, key, _) -> availableOptions[key]!! }, { (_, _, value) -> value })
    }

    private fun buildDefaultOptions(project: MavenProject, execution: MojoExecution): Map<KspCliOption, String> {
        val baseDir = project.basedir.path
        val buildDir = project.build.directory

        if (execution.mojoDescriptor.goal == "compile") {
            return mapOf(
                KspCliOption.CLASS_OUTPUT_DIR_OPTION to project.build.outputDirectory,
                KspCliOption.JAVA_OUTPUT_DIR_OPTION to path(buildDir, "generated-sources", "ksp-java"),
                KspCliOption.KOTLIN_OUTPUT_DIR_OPTION to path(buildDir, "generated-sources", "ksp"),
                KspCliOption.RESOURCE_OUTPUT_DIR_OPTION to project.build.outputDirectory,
                KspCliOption.CACHES_DIR_OPTION to path(buildDir, "kspCaches"),
                KspCliOption.PROJECT_BASE_DIR_OPTION to baseDir,
                KspCliOption.KSP_OUTPUT_DIR_OPTION to path(buildDir, "ksp"),
                KspCliOption.PROCESSOR_CLASSPATH_OPTION to buildProcessorClasspath(project, execution),
                KspCliOption.WITH_COMPILATION_OPTION to "true"
            )
        }

        if (execution.mojoDescriptor.goal == "test-compile") {
            return mapOf(
                KspCliOption.CLASS_OUTPUT_DIR_OPTION to project.build.testOutputDirectory,
                KspCliOption.JAVA_OUTPUT_DIR_OPTION to path(buildDir, "generated-test-sources", "ksp-java"),
                KspCliOption.KOTLIN_OUTPUT_DIR_OPTION to path(buildDir, "generated-test-sources", "ksp"),
                KspCliOption.RESOURCE_OUTPUT_DIR_OPTION to project.build.testOutputDirectory,
                KspCliOption.CACHES_DIR_OPTION to path(buildDir, "kspCaches"),
                KspCliOption.PROJECT_BASE_DIR_OPTION to baseDir,
                KspCliOption.KSP_OUTPUT_DIR_OPTION to path(buildDir, "ksp-test"),
                KspCliOption.PROCESSOR_CLASSPATH_OPTION to buildProcessorClasspath(project, execution),
                KspCliOption.WITH_COMPILATION_OPTION to "true"
            )
        }

        return emptyMap()
    }

    private fun path(parent: String, vararg children: String): String {
        val file = children.fold(File(parent)) { acc, child -> File(acc, child) }
        return file.path
    }

    private fun buildProcessorClasspath(project: MavenProject, execution: MojoExecution): String {
        val files = ArrayList<File>()
        for (dependency in execution.plugin.dependencies) {
            val request = ArtifactResolutionRequest()
            request.artifact = repositorySystem.createDependencyArtifact(dependency)
            request.localRepository = repositorySystem.createDefaultLocalRepository()
            request.remoteRepositories = project.pluginArtifactRepositories
            request.isResolveTransitively = true

            val resolved = repositorySystem.resolve(request)
            files += resolved.artifacts.mapNotNull { it.file }.filter { it.exists() }
        }

        return files.joinToString(File.pathSeparator) { it.path }
    }
}
