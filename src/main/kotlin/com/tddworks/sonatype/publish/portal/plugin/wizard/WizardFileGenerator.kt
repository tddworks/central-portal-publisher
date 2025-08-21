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
        // Generate gradle.properties if user manually entered credentials OR signing
        // The user explicitly chose manual input, so we should generate the file
        return !context.hasAutoDetectedCredentials || !context.hasAutoDetectedSigning
    }
    
    private fun hasGlobalCredentials(): Boolean {
        // Check if user has global gradle.properties with credentials
        val globalGradleProps = File(System.getProperty("user.home"), ".gradle/gradle.properties")
        if (!globalGradleProps.exists()) return false
        
        val content = globalGradleProps.readText()
        val hasUsername = content.contains("SONATYPE_USERNAME=") && 
                         content.lines().find { it.startsWith("SONATYPE_USERNAME=") }
                             ?.substringAfter("=")?.trim()?.isNotBlank() == true
        val hasPassword = content.contains("SONATYPE_PASSWORD=") && 
                         content.lines().find { it.startsWith("SONATYPE_PASSWORD=") }
                             ?.substringAfter("=")?.trim()?.isNotBlank() == true
        val hasSigningKey = content.contains("SIGNING_KEY=") && 
                           content.lines().find { it.startsWith("SIGNING_KEY=") }
                               ?.substringAfter("=")?.trim()?.isNotBlank() == true
        val hasSigningPassword = content.contains("SIGNING_PASSWORD=") && 
                                content.lines().find { it.startsWith("SIGNING_PASSWORD=") }
                                    ?.substringAfter("=")?.trim()?.isNotBlank() == true
        
        return (hasUsername && hasPassword) || (hasSigningKey && hasSigningPassword)
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
        
        // Use final configuration (includes manual input) with fallback to detected info
        val projectUrl = finalConfig.projectInfo.url.takeIf { it.isNotBlank() } 
            ?: context.detectedInfo?.projectUrl
            ?: "https://github.com/yourorg/${context.project.name}"
        val firstDeveloper = finalConfig.projectInfo.developers.firstOrNull()
        val detectedDeveloper = context.detectedInfo?.developers?.firstOrNull()
        val developerId = firstDeveloper?.id?.takeIf { it.isNotBlank() } 
            ?: firstDeveloper?.email?.substringBefore("@")
            ?: detectedDeveloper?.email?.substringBefore("@") 
            ?: "yourid"
        val developerName = firstDeveloper?.name?.takeIf { it.isNotBlank() } 
            ?: detectedDeveloper?.name 
            ?: "Your Name"
        val developerEmail = firstDeveloper?.email?.takeIf { it.isNotBlank() } 
            ?: detectedDeveloper?.email 
            ?: "your.email@example.com"
        
        // Generate the centralPublisher block (using original format)
        val centralPublisherBlock = buildString {
            appendLine("centralPublisher {")
            appendLine("    credentials {")
            appendLine("        username = project.findProperty(\"SONATYPE_USERNAME\")?.toString() ?: \"\"")
            appendLine("        password = project.findProperty(\"SONATYPE_PASSWORD\")?.toString() ?: \"\"")
            appendLine("    }")
            appendLine("    ")
            appendLine("    projectInfo {")
            val projectName = finalConfig.projectInfo.name.takeIf { it.isNotBlank() } 
                ?: context.detectedInfo?.projectName 
                ?: context.project.name
            val projectDescription = finalConfig.projectInfo.description.takeIf { it.isNotBlank() } 
                ?: "Description of your project"
            
            appendLine("        name = \"$projectName\"")
            appendLine("        description = \"$projectDescription\"")
            appendLine("        url = \"$projectUrl\"")
            appendLine("        ")
            appendLine("        license {")
            appendLine("            name = \"${finalConfig.projectInfo.license.name}\"")
            appendLine("            url = \"${finalConfig.projectInfo.license.url}\"")
            appendLine("        }")
            appendLine("        ")
            appendLine("        developer {")
            appendLine("            id = \"$developerId\"")
            appendLine("            name = \"$developerName\"")
            appendLine("            email = \"$developerEmail\"")
            appendLine("        }")
            appendLine("        ")
            appendLine("        scm {")
            appendLine("            url = \"$projectUrl\"")
            appendLine("            connection = \"scm:git:git://${projectUrl.removePrefix("https://")}.git\"")
            appendLine("            developerConnection = \"scm:git:ssh://${projectUrl.removePrefix("https://")}.git\"")
            appendLine("        }")
            appendLine("    }")
            appendLine("}")
        }
        
        if (buildFile.exists()) {
            // Update existing build.gradle.kts without destroying user's content
            updateExistingBuildFile(buildFile, centralPublisherBlock)
        } else {
            // Create new minimal build.gradle.kts
            createNewBuildFile(buildFile, centralPublisherBlock)
        }
    }
    
    private fun updateExistingBuildFile(buildFile: File, centralPublisherBlock: String) {
        val existingContent = buildFile.readText()
        
        // Check if central-publisher plugin is already in plugins block
        val hasPlugin = existingContent.contains("id(\"com.tddworks.central-publisher\")")
        
        val updatedContent = if (existingContent.contains("centralPublisher {")) {
            // Replace existing centralPublisher block
            replaceCentralPublisherBlock(existingContent, centralPublisherBlock)
        } else {
            // Add centralPublisher block at the end
            if (hasPlugin) {
                // Plugin exists, just add the block
                "$existingContent\n\n$centralPublisherBlock"
            } else {
                // Need to add both plugin and block
                addPluginAndBlock(existingContent, centralPublisherBlock)
            }
        }
        
        buildFile.writeText(updatedContent)
    }
    
    private fun createNewBuildFile(buildFile: File, centralPublisherBlock: String) {
        val content = buildString {
            appendLine("plugins {")
            appendLine("    id(\"com.tddworks.central-publisher\")")
            appendLine("}")
            appendLine()
            appendLine(centralPublisherBlock)
        }
        buildFile.writeText(content)
    }
    
    private fun replaceCentralPublisherBlock(content: String, newBlock: String): String {
        // Find the centralPublisher block and replace it
        val regex = Regex(
            "centralPublisher\\s*\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}",
            RegexOption.DOT_MATCHES_ALL
        )
        return regex.replace(content, newBlock.trim())
    }
    
    private fun addPluginAndBlock(content: String, centralPublisherBlock: String): String {
        // Try to add plugin to existing plugins block
        val pluginsRegex = Regex("plugins\\s*\\{([^}]*)\\}")
        val pluginsMatch = pluginsRegex.find(content)
        
        return if (pluginsMatch != null) {
            // Add to existing plugins block
            val pluginsContent = pluginsMatch.groupValues[1]
            val newPluginsContent = "$pluginsContent\n    id(\"com.tddworks.central-publisher\")"
            content.replace(
                pluginsMatch.value,
                "plugins {\n$newPluginsContent\n}"
            ) + "\n\n$centralPublisherBlock"
        } else {
            // No plugins block exists, add one at the beginning
            "plugins {\n    id(\"com.tddworks.central-publisher\")\n}\n\n$content\n\n$centralPublisherBlock"
        }
    }
}

/**
 * Extended file generator that includes additional files like .gitignore and CI workflows
 */
class DefaultWizardFileGeneratorWithExtras : WizardFileGenerator {
    private val baseGenerator = DefaultWizardFileGenerator()
    
    override fun generateFiles(context: WizardContext, finalConfig: CentralPublisherConfig): List<String> {
        val generatedFiles = baseGenerator.generateFiles(context, finalConfig).toMutableList()
        
        // Generate .gitignore
        generateGitignore(context)
        generatedFiles.add(".gitignore")
        
        // Generate CI workflow
        generateCIWorkflow(context)
        generatedFiles.add(".github/workflows/publish.yml")
        
        return generatedFiles
    }
    
    private fun generateGitignore(context: WizardContext) {
        val gitignoreFile = File(context.project.projectDir, ".gitignore")
        
        val content = """
            # Compiled class file
            *.class
            
            # Log file
            *.log
            
            # BlueJ files
            *.ctxt
            
            # Mobile Tools for Java (J2ME)
            .mtj.tmp/
            
            # Package Files #
            *.jar
            *.war
            *.nar
            *.ear
            *.zip
            *.tar.gz
            *.rar
            
            # Virtual machine crash logs
            hs_err_pid*
            replay_pid*
            
            # Gradle
            .gradle
            build/
            !gradle/wrapper/gradle-wrapper.jar
            !**/src/main/**/build/
            !**/src/test/**/build/
            
            # IntelliJ IDEA
            .idea
            *.iws
            *.iml
            *.ipr
            out/
            !**/src/main/**/out/
            !**/src/test/**/out/
            
            # Eclipse
            .apt_generated
            .classpath
            .factorypath
            .project
            .settings
            .springBeans
            .sts4-cache
            bin/
            !**/src/main/**/bin/
            !**/src/test/**/bin/
            
            # NetBeans
            /nbproject/private/
            /nbbuild/
            /dist/
            /nbdist/
            /.nb-gradle/
            
            # VS Code
            .vscode/
            
            # Mac
            .DS_Store
            
            # Sensitive files - DO NOT COMMIT
            gradle.properties
            local.properties
            *.gpg
            *.p12
            *.jks
            *.keystore
            
        """.trimIndent()
        
        gitignoreFile.writeText(content)
    }
    
    private fun generateCIWorkflow(context: WizardContext) {
        val workflowDir = File(context.project.projectDir, ".github/workflows")
        workflowDir.mkdirs()
        val workflowFile = File(workflowDir, "publish.yml")
        
        val content = """
            name: Publish to Maven Central
            
            on:
              push:
                tags:
                  - 'v*'
              workflow_dispatch:
            
            jobs:
              publish:
                runs-on: ubuntu-latest
                
                steps:
                - name: Checkout code
                  uses: actions/checkout@v4
                  
                - name: Set up JDK 17
                  uses: actions/setup-java@v4
                  with:
                    java-version: '17'
                    distribution: 'temurin'
                    
                - name: Setup Gradle
                  uses: gradle/gradle-build-action@v2
                  
                - name: Publish to Maven Central
                  run: ./gradlew centralPublish
                  env:
                    SONATYPE_USERNAME: ${'$'}{{ secrets.SONATYPE_USERNAME }}
                    SONATYPE_PASSWORD: ${'$'}{{ secrets.SONATYPE_PASSWORD }}
                    SIGNING_KEY: ${'$'}{{ secrets.SIGNING_KEY }}
                    SIGNING_PASSWORD: ${'$'}{{ secrets.SIGNING_PASSWORD }}
            
        """.trimIndent()
        
        workflowFile.writeText(content)
    }
}