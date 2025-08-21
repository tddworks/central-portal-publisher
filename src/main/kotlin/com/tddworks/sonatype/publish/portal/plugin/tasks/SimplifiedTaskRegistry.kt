package com.tddworks.sonatype.publish.portal.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin
import com.tddworks.sonatype.publish.portal.plugin.error.PublishingErrorSystem
import com.tddworks.sonatype.publish.portal.plugin.error.ErrorCode
import com.tddworks.sonatype.publish.portal.plugin.error.ErrorContext

/**
 * Registry for simplified task names that map to the original verbose task names.
 * This provides a more user-friendly interface while maintaining backward compatibility.
 */
class SimplifiedTaskRegistry {
    
    companion object {
        const val CENTRAL_PUBLISHING_GROUP = "Central Publishing"
        
        // New simplified task names
        const val PUBLISH_TO_CENTRAL = "publishToCentral"
        const val BUNDLE_ARTIFACTS = "bundleArtifacts"
        const val VALIDATE_PUBLISHING = "validatePublishing"
        const val SETUP_PUBLISHING = "setupPublishing"
        
        // Task name mappings (old -> new)
        private val TASK_MAPPINGS = mapOf(
            SonatypePortalPublisherPlugin.PUBLISH_ALL_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY to PUBLISH_TO_CENTRAL,
            SonatypePortalPublisherPlugin.PUBLISH_AGGREGATION_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY to PUBLISH_TO_CENTRAL,
            SonatypePortalPublisherPlugin.ZIP_ALL_PUBLICATIONS to BUNDLE_ARTIFACTS,
            SonatypePortalPublisherPlugin.ZIP_AGGREGATION_PUBLICATIONS to BUNDLE_ARTIFACTS,
            "publishMavenPublicationToSonatypePortalRepository" to "publishMaven",
            "publishKotlinMultiplatformPublicationToSonatypePortalRepository" to "publishKMP",
            "zipMavenPublication" to "bundleMaven",
            "zipKotlinMultiplatformPublication" to "bundleKMP"
        )
        
        fun getTaskMappings(): Map<String, String> = TASK_MAPPINGS
        
        fun getNewTaskName(oldTaskName: String): String = TASK_MAPPINGS[oldTaskName] ?: oldTaskName
    }
    
    /**
     * Registers simplified task names that delegate to the original tasks.
     * This maintains backward compatibility while providing a cleaner interface.
     */
    fun registerSimplifiedTasks(project: Project) {
        // Register publishToCentral task
        registerPublishToCentralTask(project)
        
        // Register bundleArtifacts task
        registerBundleArtifactsTask(project)
        
        // Register validatePublishing task
        registerValidatePublishingTask(project)
        
        // Add deprecation warnings to old tasks
        addDeprecationWarnings(project)
    }
    
    private fun registerPublishToCentralTask(project: Project) {
        project.tasks.register(PUBLISH_TO_CENTRAL, DefaultTask::class.java) {
            group = CENTRAL_PUBLISHING_GROUP
            description = "Publishes all artifacts to Maven Central"
            
            doLast {
                // This will be replaced by the actual implementation
                // For now, we depend on afterEvaluate to wire dependencies
                val oldPublishTask = project.tasks.findByName(SonatypePortalPublisherPlugin.PUBLISH_ALL_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY)
                val oldAggregateTask = project.tasks.findByName(SonatypePortalPublisherPlugin.PUBLISH_AGGREGATION_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY)
                
                when {
                    oldAggregateTask != null -> logger.lifecycle("Delegating to: ${oldAggregateTask.name}")
                    oldPublishTask != null -> logger.lifecycle("Delegating to: ${oldPublishTask.name}")
                    else -> {
                        logger.lifecycle("Publishing to Maven Central...")
                        logger.lifecycle("Note: This task will delegate to the actual publishing implementation")
                    }
                }
            }
        }
        
        // Wire dependencies after evaluation if possible
        if (project.state.executed) {
            // Project already evaluated, wire immediately
            wireDependenciesForPublishTask(project)
        } else {
            // Project not yet evaluated, wire later
            project.afterEvaluate { 
                wireDependenciesForPublishTask(this)
            }
        }
    }
    
    private fun registerBundleArtifactsTask(project: Project) {
        project.tasks.register(BUNDLE_ARTIFACTS, DefaultTask::class.java) {
            group = CENTRAL_PUBLISHING_GROUP
            description = "Creates deployment bundles for all artifacts"
            
            doLast {
                val oldZipTask = project.tasks.findByName(SonatypePortalPublisherPlugin.ZIP_ALL_PUBLICATIONS)
                val oldAggregateZipTask = project.tasks.findByName(SonatypePortalPublisherPlugin.ZIP_AGGREGATION_PUBLICATIONS)
                
                when {
                    oldAggregateZipTask != null -> logger.lifecycle("Delegating to: ${oldAggregateZipTask.name}")
                    oldZipTask != null -> logger.lifecycle("Delegating to: ${oldZipTask.name}")
                    else -> {
                        logger.lifecycle("Creating deployment bundles...")
                        logger.lifecycle("Note: This task will delegate to the actual bundling implementation")
                    }
                }
            }
        }
        
        // Wire dependencies after evaluation if possible
        if (project.state.executed) {
            // Project already evaluated, wire immediately
            wireDependenciesForBundleTask(project)
        } else {
            // Project not yet evaluated, wire later
            project.afterEvaluate { 
                wireDependenciesForBundleTask(this)
            }
        }
    }
    
    private fun registerValidatePublishingTask(project: Project) {
        project.tasks.register(VALIDATE_PUBLISHING, PublishingValidationTask::class.java) {
            group = CENTRAL_PUBLISHING_GROUP
            description = "Validates publishing configuration and credentials"
        }
    }
    
    private fun wireDependenciesForPublishTask(project: Project) {
        val publishToCentralTask = project.tasks.findByName(PUBLISH_TO_CENTRAL)
        val oldPublishTask = project.tasks.findByName(SonatypePortalPublisherPlugin.PUBLISH_ALL_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY)
        val oldAggregateTask = project.tasks.findByName(SonatypePortalPublisherPlugin.PUBLISH_AGGREGATION_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY)
        
        publishToCentralTask?.let { task ->
            when {
                oldAggregateTask != null -> task.dependsOn(oldAggregateTask)
                oldPublishTask != null -> task.dependsOn(oldPublishTask)
                else -> { /* No old tasks to depend on */ }
            }
        }
    }
    
    private fun wireDependenciesForBundleTask(project: Project) {
        val bundleArtifactsTask = project.tasks.findByName(BUNDLE_ARTIFACTS)
        val oldZipTask = project.tasks.findByName(SonatypePortalPublisherPlugin.ZIP_ALL_PUBLICATIONS)
        val oldAggregateZipTask = project.tasks.findByName(SonatypePortalPublisherPlugin.ZIP_AGGREGATION_PUBLICATIONS)
        
        bundleArtifactsTask?.let { task ->
            when {
                oldAggregateZipTask != null -> task.dependsOn(oldAggregateZipTask)
                oldZipTask != null -> task.dependsOn(oldZipTask)
                else -> { /* No old tasks to depend on */ }
            }
        }
    }
    
    private fun addDeprecationWarnings(project: Project) {
        if (project.state.executed) {
            addDeprecationWarningsImpl(project)
        } else {
            project.afterEvaluate {
                addDeprecationWarningsImpl(this)
            }
        }
    }
    
    private fun addDeprecationWarningsImpl(project: Project) {
        TASK_MAPPINGS.forEach { (oldName, newName) ->
            project.tasks.findByName(oldName)?.let { oldTask ->
                oldTask.doFirst {
                    logger.warn("""
                        |
                        |========================================
                        |DEPRECATION WARNING
                        |========================================
                        |Task '$oldName' is deprecated and will be removed in a future version.
                        |Please use '$newName' instead.
                        |
                        |To update your build script:
                        |  OLD: ./gradlew $oldName
                        |  NEW: ./gradlew $newName
                        |========================================
                        |
                    """.trimMargin())
                }
            }
        }
    }
}

/**
 * Task for validating publishing configuration before attempting to publish.
 * Uses the structured error system for consistent reporting.
 */
abstract class PublishingValidationTask : DefaultTask() {
    
    private val errorSystem = PublishingErrorSystem()
    
    init {
        outputs.upToDateWhen { false } // Always run validation
    }
    
    @TaskAction
    fun validate() {
        logger.lifecycle("Validating publishing configuration...")
        
        val context = ErrorContext(
            projectPath = project.path,
            taskName = name
        )
        
        val errors = mutableListOf<com.tddworks.sonatype.publish.portal.plugin.error.PublishingError>()
        
        // Validate all aspects of configuration
        validateCredentials(context, errors)
        validateProjectInfo(context, errors)  
        validateSigning(context, errors)
        
        // Generate report
        val report = errorSystem.createErrorReport(errors)
        
        if (report.summary.totalErrors > 0 || report.summary.totalWarnings > 0) {
            logger.lifecycle(report.formattedOutput)
        }
        
        if (report.summary.totalErrors > 0) {
            throw org.gradle.api.GradleException(
                "Publishing validation failed with ${report.summary.totalErrors} error(s). Please fix the issues above."
            )
        }
        
        logger.lifecycle("✅ Publishing configuration is valid!")
    }
    
    private fun validateCredentials(context: ErrorContext, errors: MutableList<com.tddworks.sonatype.publish.portal.plugin.error.PublishingError>) {
        val username = project.findProperty("SONATYPE_USERNAME") as? String
            ?: System.getenv("SONATYPE_USERNAME")
        val password = project.findProperty("SONATYPE_PASSWORD") as? String
            ?: System.getenv("SONATYPE_PASSWORD")
        
        if (username.isNullOrBlank()) {
            errors.add(errorSystem.createError(
                ErrorCode.MISSING_CREDENTIALS,
                "SONATYPE_USERNAME is not configured",
                context
            ))
        }
        
        if (password.isNullOrBlank()) {
            errors.add(errorSystem.createError(
                ErrorCode.MISSING_CREDENTIALS,
                "SONATYPE_PASSWORD is not configured",
                context
            ))
        }
    }
    
    private fun validateProjectInfo(context: ErrorContext, errors: MutableList<com.tddworks.sonatype.publish.portal.plugin.error.PublishingError>) {
        val pomDescription = project.findProperty("POM_DESCRIPTION") as? String
        
        if (pomDescription.isNullOrBlank()) {
            errors.add(errorSystem.createWarning(
                ErrorCode.MISSING_DESCRIPTION,
                "POM_DESCRIPTION is not set. Consider adding a description for better discoverability",
                context
            ))
        }
    }
    
    private fun validateSigning(context: ErrorContext, errors: MutableList<com.tddworks.sonatype.publish.portal.plugin.error.PublishingError>) {
        val signingKey = project.findProperty("signing.keyId") as? String
            ?: System.getenv("SIGNING_KEY")
        val signingPassword = project.findProperty("signing.password") as? String
            ?: System.getenv("SIGNING_PASSWORD")
        
        if (signingKey.isNullOrBlank()) {
            errors.add(errorSystem.createError(
                ErrorCode.MISSING_SIGNING_KEY,
                "GPG signing key is not configured",
                context
            ))
        }
        
        if (signingPassword.isNullOrBlank()) {
            errors.add(errorSystem.createError(
                ErrorCode.MISSING_SIGNING_KEY,
                "GPG signing password is not configured",
                context
            ))
        }
    }
}