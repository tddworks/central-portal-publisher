package com.tddworks.sonatype.publish.portal.plugin.wizard

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import java.io.File

/**
 * Interface for generating wizard files
 */
interface WizardFileGenerator {
    /**
     * Generate configuration files based on wizard context and final configuration
     */
    fun generateFiles(context: WizardContext, finalConfig: CentralPublisherConfig): List<String>
}

/**
 * Default implementation of WizardFileGenerator
 */
class DefaultWizardFileGenerator : WizardFileGenerator {
    
    override fun generateFiles(context: WizardContext, finalConfig: CentralPublisherConfig): List<String> {
        val generatedFiles = mutableListOf<String>()
        
        // Generate gradle.properties only if needed
        if (shouldGenerateGradleProperties(context)) {
            generateGradleProperties(context, finalConfig)
            generatedFiles.add("gradle.properties")
        }
        
        // Always generate build.gradle.kts
        generateBuildFile(context, finalConfig)
        generatedFiles.add("build.gradle.kts")
        
        return generatedFiles
    }
    
    private fun shouldGenerateGradleProperties(context: WizardContext): Boolean {
        return !context.hasAutoDetectedCredentials || !context.hasAutoDetectedSigning
    }
    
    private fun generateGradleProperties(context: WizardContext, finalConfig: CentralPublisherConfig) {
        val gradlePropsFile = File(context.project.projectDir, "gradle.properties")
        
        val content = buildString {
            appendLine("# Central Publisher Configuration")
            appendLine("# WARNING: This file contains sensitive information.")
            appendLine("# Do not commit this file to version control.")
            appendLine("# Consider using environment variables or global gradle.properties instead.")
            appendLine()
            
            // Only add credentials section if not auto-detected
            if (!context.hasAutoDetectedCredentials) {
                appendLine("# Central Publisher Credentials")
                appendLine("# Get these from https://central.sonatype.org/")
                appendLine("SONATYPE_USERNAME=${finalConfig.credentials.username}")
                appendLine("SONATYPE_PASSWORD=${finalConfig.credentials.password}")
                appendLine()
            }
            
            // Only add signing section if not auto-detected
            if (!context.hasAutoDetectedSigning) {
                appendLine("# Central Publisher Signing")
                appendLine("# GPG signing configuration")
                appendLine("SIGNING_KEY=${finalConfig.signing.keyId}")
                appendLine("SIGNING_PASSWORD=${finalConfig.signing.password}")
                appendLine()
            }
            
            appendLine("# Additional Gradle properties can be added here")
        }
        
        gradlePropsFile.writeText(content)
    }
    
    private fun generateBuildFile(context: WizardContext, finalConfig: CentralPublisherConfig) {
        val buildFile = File(context.project.projectDir, "build.gradle.kts")
        val detectedInfo = context.detectedInfo
        
        // Use auto-detected information or fallback to defaults
        val projectUrl = detectedInfo?.projectUrl ?: "https://github.com/yourorg/${context.project.name}"
        val firstDeveloper = detectedInfo?.developers?.firstOrNull()
        val developerId = firstDeveloper?.email?.substringBefore("@") ?: "yourid"
        val developerName = firstDeveloper?.name ?: "Your Name"
        val developerEmail = firstDeveloper?.email ?: "your.email@example.com"
        
        val content = buildString {
            appendLine("plugins {")
            appendLine("    kotlin(\"jvm\") version \"1.9.23\"")
            appendLine("    id(\"com.tddworks.sonatype-portal-publisher\") version \"1.0.0\"")
            appendLine("}")
            appendLine()
            appendLine("group = \"com.example\"") // TODO: Add groupId to config model
            appendLine("version = \"1.0.0\"") // TODO: Add version to config model
            appendLine("description = \"${finalConfig.projectInfo.description}\"")
            appendLine()
            appendLine("repositories {")
            appendLine("    mavenCentral()")
            appendLine("}")
            appendLine()
            appendLine("dependencies {")
            appendLine("    // Add your dependencies here")
            appendLine("}")
            appendLine()
            appendLine("sonatypePortalPublisher {")
            appendLine("    projectInfo {")
            appendLine("        name.set(\"${finalConfig.projectInfo.name}\")")
            appendLine("        description.set(\"${finalConfig.projectInfo.description}\")")
            appendLine("        url.set(\"$projectUrl\")")
            appendLine("        license {")
            appendLine("            name.set(\"${finalConfig.projectInfo.license.name}\")")
            appendLine("            url.set(\"${finalConfig.projectInfo.license.url}\")")
            appendLine("        }")
            appendLine("        developer {")
            appendLine("            id.set(\"$developerId\")")
            appendLine("            name.set(\"$developerName\")")
            appendLine("            email.set(\"$developerEmail\")")
            appendLine("        }")
            appendLine("        scm {")
            appendLine("            connection.set(\"scm:git:git://github.com/yourorg/${context.project.name}.git\")")
            appendLine("            developerConnection.set(\"scm:git:ssh://github.com:yourorg/${context.project.name}.git\")")
            appendLine("            url.set(\"$projectUrl\")")
            appendLine("        }")
            appendLine("    }")
            appendLine("    publishing {")
            appendLine("        autoPublish.set(${finalConfig.publishing.autoPublish})")
            appendLine("        aggregation.set(${finalConfig.publishing.aggregation})")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("// Additional build configuration can be added here")
        }
        
        buildFile.writeText(content)
    }
}