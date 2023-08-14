package org.ktorm.ksp.compiler.maven

import com.google.devtools.ksp.KspCliOption
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.apache.maven.repository.RepositorySystem
import org.codehaus.plexus.component.annotations.Component
import org.codehaus.plexus.component.annotations.Requirement
import org.jetbrains.kotlin.maven.KotlinMavenPluginExtension
import org.jetbrains.kotlin.maven.PluginOption

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
        return true
    }

    override fun getPluginOptions(project: MavenProject, execution: MojoExecution): List<PluginOption> {
//        val request = ArtifactResolutionRequest()
//        request.artifact = repositorySystem.createArtifactWithClassifier("com.pinterest", "ktlint", "0.50.0", "jar", "all")
//        request.remoteRepositories = project.remoteArtifactRepositories
//
//        val resolved = repositorySystem.resolve(request)
//        throw MojoExecutionException("test ktorm ksp: ${resolved.artifacts.map { it.file }}")
        return emptyList()
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
}
