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

package org.ktorm.ksp.compiler.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Maven plugin MOJO that handles Ktorm KSP code generation.
 */
@Mojo(
    name = "generate-sources",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
public class GenerateSourcesMojo extends AbstractGenerateSourcesMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter
    private Map<String, String> processorOptions;

    @NotNull
    @Override
    public MavenProject getProject() {
        return project;
    }

    @NotNull
    @Override
    public Map<String, String> getProcessorOptions() {
        return processorOptions != null ? processorOptions : Collections.emptyMap();
    }

    @NotNull
    @Override
    protected List<File> getSourceRoots() {
        return project.getCompileSourceRoots().stream().map(File::new).collect(toList());
    }

    @NotNull
    @Override
    protected List<File> getLibraries() {
        try {
            return project.getCompileClasspathElements().stream().map(File::new).collect(toList());
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    @Override
    protected File getSourceOutputDirectory() {
        return new File(new File(project.getBuild().getDirectory(), "generated-sources"), "ktorm");
    }

    @NotNull
    @Override
    protected File getOutputDirectory() {
        return new File(project.getBuild().getOutputDirectory());
    }

    @NotNull
    @Override
    protected File getCachesDirectory() {
        return new File(new File(project.getBuild().getDirectory(), "ktorm"), "ksp-caches");
    }

    @Nullable
    @Override
    protected Xpp3Dom getKotlinCompileMojoConfiguration() {
        Plugin plugin = project.getBuild().getPluginsAsMap().get("org.jetbrains.kotlin:kotlin-maven-plugin");
        if (plugin == null) {
            return null;
        }

        Xpp3Dom configuration = null;
        for (PluginExecution execution : plugin.getExecutions()) {
            if (execution.getPhase().equals("compile")) {
                configuration = Xpp3Dom.mergeXpp3Dom((Xpp3Dom) execution.getConfiguration(), configuration);
            }
        }

        return configuration;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<File> sourceDirs = generateSources();
        for (File sourceDir : sourceDirs) {
            project.addCompileSourceRoot(sourceDir.getPath());
        }
    }
}
