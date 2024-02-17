package com.tddworks.sonatype.publish.portal.api.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Aggregates the modules to be published.
 * @param configuration The configuration to add the modules to.
 * @param project The project to add the modules to.
 */
class ModulesAggregation(
    private val configuration: Configuration,
    private val project: Project,
) {
    /**
     * Adds a module to the configuration.
     * @param dependency The path to the module.
     */
    fun project(dependency: String) {
        val dependencyProjectPath = project.dependencies.project(mapOf("path" to dependency))
        project.dependencies.add(configuration.name, dependencyProjectPath)
    }

}