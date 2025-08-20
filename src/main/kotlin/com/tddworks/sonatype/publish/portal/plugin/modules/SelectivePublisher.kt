package com.tddworks.sonatype.publish.portal.plugin.modules

/**
 * Handles selective publishing of modules in multi-module projects.
 * 
 * Provides functionality to:
 * - Filter modules using include/exclude patterns
 * - Apply custom module filters based on various criteria
 * - Support module groups for logical organization
 * - Validate module selection configuration
 */
class SelectivePublisher {

    /**
     * Selects modules from the given module structure based on the selection configuration.
     * 
     * @param moduleStructure The analyzed module structure
     * @param config The module selection configuration
     * @return List of selected modules that match the criteria
     */
    fun selectModules(moduleStructure: ModuleStructure, config: ModuleSelectionConfig): List<GradleModule> {
        // In multi-module projects, exclude root module if it's not publishable by default
        var candidates = if (moduleStructure.isMultiModule && !moduleStructure.rootModule.isPublishable) {
            moduleStructure.subModules
        } else {
            moduleStructure.allModules
        }
        
        // Apply include patterns if specified
        if (config.includePatterns.isNotEmpty()) {
            candidates = candidates.filter { module ->
                config.includePatterns.any { pattern ->
                    matchesPattern(module.name, pattern)
                }
            }
        }
        
        // Apply exclude patterns if specified
        if (config.excludePatterns.isNotEmpty()) {
            candidates = candidates.filter { module ->
                config.excludePatterns.none { pattern ->
                    matchesPattern(module.name, pattern)
                }
            }
        }
        
        // Apply module groups if specified
        if (config.selectedGroups.isNotEmpty()) {
            val groupedModules = config.selectedGroups.flatMap { groupName ->
                config.groups[groupName] ?: emptyList()
            }
            candidates = candidates.filter { module ->
                groupedModules.contains(module.name)
            }
        }
        
        // Apply custom filters
        config.filters.forEach { filter ->
            candidates = filter.apply(candidates)
        }
        
        return candidates
    }

    /**
     * Validates the module selection configuration against the module structure.
     * 
     * @param moduleStructure The analyzed module structure
     * @param config The module selection configuration to validate
     * @return Validation result with errors if any
     */
    fun validateSelection(moduleStructure: ModuleStructure, config: ModuleSelectionConfig): ValidationResult {
        val errors = mutableListOf<String>()
        val allModuleNames = moduleStructure.allModules.map { it.name }.toSet()
        
        // Validate include patterns reference existing modules
        config.includePatterns.forEach { pattern ->
            if (!isWildcardPattern(pattern) && !allModuleNames.contains(pattern)) {
                errors.add("Include pattern '$pattern' does not match any existing module")
            }
        }
        
        // Validate group references
        config.selectedGroups.forEach { groupName ->
            if (!config.groups.containsKey(groupName)) {
                errors.add("Selected group '$groupName' is not defined in groups configuration")
            }
        }
        
        // Validate group module references
        config.groups.values.flatten().forEach { moduleName ->
            if (!allModuleNames.contains(moduleName)) {
                errors.add("Group references non-existent module '$moduleName'")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun matchesPattern(name: String, pattern: String): Boolean {
        return if (isWildcardPattern(pattern)) {
            val regex = pattern
                .replace("*", ".*")
                .replace("?", ".")
                .toRegex()
            regex.matches(name)
        } else {
            name == pattern
        }
    }

    private fun isWildcardPattern(pattern: String): Boolean {
        return pattern.contains("*") || pattern.contains("?")
    }
}

/**
 * Configuration for module selection in multi-module projects.
 */
data class ModuleSelectionConfig(
    val includePatterns: List<String> = emptyList(),
    val excludePatterns: List<String> = emptyList(),
    val filters: List<ModuleFilter> = emptyList(),
    val groups: Map<String, List<String>> = emptyMap(),
    val selectedGroups: List<String> = emptyList()
)

/**
 * Interface for custom module filtering logic.
 */
sealed class ModuleFilter {
    
    /**
     * Applies the filter to the list of candidate modules.
     */
    abstract fun apply(modules: List<GradleModule>): List<GradleModule>

    /**
     * Filter that only includes publishable modules.
     */
    data class PublishableOnly(private val publishableOnly: Boolean) : ModuleFilter() {
        override fun apply(modules: List<GradleModule>): List<GradleModule> {
            return if (publishableOnly) {
                modules.filter { it.isPublishable }
            } else {
                modules.filter { !it.isPublishable }
            }
        }
    }

    /**
     * Filter that includes/excludes modules based on dependency presence.
     */
    data class HasDependencies(
        private val hasProjectDependencies: Boolean
    ) : ModuleFilter() {
        override fun apply(modules: List<GradleModule>): List<GradleModule> {
            return if (hasProjectDependencies) {
                modules.filter { it.dependencies.isNotEmpty() }
            } else {
                modules.filter { it.dependencies.isEmpty() }
            }
        }
    }

    /**
     * Filter that includes modules based on path patterns.
     */
    data class PathPattern(private val patterns: List<String>) : ModuleFilter() {
        override fun apply(modules: List<GradleModule>): List<GradleModule> {
            return modules.filter { module ->
                patterns.any { pattern ->
                    matchesPattern(module.path, pattern)
                }
            }
        }

        private fun matchesPattern(path: String, pattern: String): Boolean {
            val regex = pattern.replace("*", ".*").replace("?", ".").toRegex()
            return regex.matches(path)
        }
    }

    /**
     * Filter that includes modules based on directory structure.
     */
    data class DirectoryPattern(private val patterns: List<String>) : ModuleFilter() {
        override fun apply(modules: List<GradleModule>): List<GradleModule> {
            return modules.filter { module ->
                val relativePath = module.directory.name
                patterns.any { pattern ->
                    matchesPattern(relativePath, pattern)
                }
            }
        }

        private fun matchesPattern(name: String, pattern: String): Boolean {
            val regex = pattern.replace("*", ".*").replace("?", ".").toRegex()
            return regex.matches(name)
        }
    }
}

/**
 * Result of validating module selection configuration.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)