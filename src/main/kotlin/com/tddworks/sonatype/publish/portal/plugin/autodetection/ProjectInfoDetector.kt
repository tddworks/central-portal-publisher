package com.tddworks.sonatype.publish.portal.plugin.autodetection

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfigBuilder
import com.tddworks.sonatype.publish.portal.plugin.config.ConfigurationSource
import org.gradle.api.Project
import java.io.File

/**
 * Detects project information from build files and project structure
 */
class ProjectInfoDetector : AutoDetector {
    
    override val name = "ProjectInfoDetector"
    
    override fun detect(project: Project): DetectionResult? {
        val detectedValues = mutableMapOf<String, DetectedValue>()
        val warnings = mutableListOf<String>()
        val configBuilder = CentralPublisherConfigBuilder().withSource(ConfigurationSource.AUTO_DETECTED)
        
        // Detect project name
        val projectName = detectProjectName(project)
        if (projectName != null) {
            configBuilder.projectInfo { name = projectName.value }
            detectedValues["projectInfo.name"] = projectName
        }
        
        // Detect project description
        val projectDescription = detectProjectDescription(project)
        if (projectDescription != null) {
            configBuilder.projectInfo { description = projectDescription.value }
            detectedValues["projectInfo.description"] = projectDescription
        }
        
        // Warn about missing information
        if (projectName == null) {
            warnings.add("Could not auto-detect project name from build.gradle.kts")
        }
        
        if (projectDescription == null) {
            warnings.add("Could not auto-detect project description from build.gradle.kts or README files")
        }
        
        return if (detectedValues.isNotEmpty() || warnings.isNotEmpty()) {
            DetectionResult(
                config = configBuilder.build(),
                detectedValues = detectedValues,
                warnings = warnings
            )
        } else {
            null
        }
    }
    
    private fun detectProjectName(project: Project): DetectedValue? {
        // Try Gradle project name first (highest confidence)
        if (project.name != "root" && project.name.isNotBlank()) {
            return DetectedValue(
                path = "projectInfo.name",
                value = project.name,
                source = "Gradle project",
                confidence = Confidence.HIGH
            )
        }
        
        // Fall back to directory name (medium confidence)
        val dirName = project.projectDir.name
        if (dirName.isNotBlank() && dirName != "." && dirName != ".." && !dirName.startsWith("tmp")) {
            return DetectedValue(
                path = "projectInfo.name", 
                value = dirName,
                source = "Directory name",
                confidence = Confidence.MEDIUM
            )
        }
        
        return null
    }
    
    private fun detectProjectDescription(project: Project): DetectedValue? {
        // Try to extract description from build.gradle.kts
        val buildFile = File(project.projectDir, "build.gradle.kts")
        if (buildFile.exists()) {
            val description = extractDescriptionFromBuildScript(buildFile)
            if (description != null) {
                return DetectedValue(
                    path = "projectInfo.description",
                    value = description,
                    source = "build.gradle.kts",
                    confidence = Confidence.HIGH
                )
            }
        }
        
        // Try build.gradle (Groovy)
        val groovyBuildFile = File(project.projectDir, "build.gradle")
        if (groovyBuildFile.exists()) {
            val description = extractDescriptionFromGroovyBuildScript(groovyBuildFile)
            if (description != null) {
                return DetectedValue(
                    path = "projectInfo.description",
                    value = description,
                    source = "build.gradle",
                    confidence = Confidence.HIGH
                )
            }
        }
        
        // Try README files
        val readmeFiles = listOf("README.md", "README.txt", "README.rst", "README")
        for (readmeFile in readmeFiles) {
            val file = File(project.projectDir, readmeFile)
            if (file.exists()) {
                val description = extractDescriptionFromReadme(file)
                if (description != null) {
                    return DetectedValue(
                        path = "projectInfo.description",
                        value = description,
                        source = readmeFile,
                        confidence = Confidence.MEDIUM
                    )
                }
            }
        }
        
        return null
    }
    
    private fun extractDescriptionFromBuildScript(buildFile: File): String? {
        try {
            val content = buildFile.readText()
            
            // Look for description property
            val descriptionRegex = Regex("""description\s*=\s*["']([^"']+)["']""")
            val match = descriptionRegex.find(content)
            if (match != null) {
                return match.groupValues[1].trim()
            }
            
            // Look for project description block
            val projectBlockRegex = Regex("""project\s*\{\s*description\s*=\s*["']([^"']+)["']""", RegexOption.DOT_MATCHES_ALL)
            val projectMatch = projectBlockRegex.find(content)
            if (projectMatch != null) {
                return projectMatch.groupValues[1].trim()
            }
            
        } catch (e: Exception) {
            // Ignore file reading errors
        }
        
        return null
    }
    
    private fun extractDescriptionFromGroovyBuildScript(buildFile: File): String? {
        try {
            val content = buildFile.readText()
            
            // Look for description property (similar patterns work for both Kotlin and Groovy)
            val patterns = listOf(
                Regex("""description\s*=\s*['"]([^'"]+)['"]"""),
                Regex("""description\s*=\s*'([^']+)'"""),
                Regex("""description\s*=\s*"([^"]+)"""")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(content)
                if (match != null) {
                    return match.groupValues[1].trim()
                }
            }
            
        } catch (e: Exception) {
            // Ignore file reading errors
        }
        
        return null
    }
    
    private fun extractDescriptionFromReadme(readmeFile: File): String? {
        try {
            val lines = readmeFile.readLines()
            if (lines.isEmpty()) return null
            
            // For markdown files, look for the first paragraph after the title
            if (readmeFile.name.endsWith(".md") || readmeFile.name.endsWith(".markdown")) {
                var foundTitle = false
                for (line in lines) {
                    val trimmed = line.trim()
                    
                    // Skip empty lines
                    if (trimmed.isEmpty()) continue
                    
                    // Skip title lines (starting with #)
                    if (trimmed.startsWith("#")) {
                        foundTitle = true
                        continue
                    }
                    
                    // If we found a title and this is content, use it as description
                    if (foundTitle && trimmed.isNotBlank() && !trimmed.startsWith("[![") && !trimmed.startsWith("[!")) {
                        return trimmed.take(200) // Limit to reasonable length
                    }
                    
                    // If no title found but we have content, use the first meaningful line
                    if (!foundTitle && trimmed.isNotBlank() && !trimmed.startsWith("[![") && !trimmed.startsWith("[!")) {
                        return trimmed.take(200)
                    }
                }
            } else {
                // For plain text README, use the first non-empty line that looks like content
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank() && trimmed.length > 10) { // Avoid single words
                        return trimmed.take(200)
                    }
                }
            }
            
        } catch (e: Exception) {
            // Ignore file reading errors
        }
        
        return null
    }
}