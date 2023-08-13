package org.ktorm.ksp.compiler.maven

import org.apache.maven.plugin.MojoExecution
import org.apache.maven.plugin.MojoExecutionException
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
        throw MojoExecutionException("test ktorm ksp")
    }
}
