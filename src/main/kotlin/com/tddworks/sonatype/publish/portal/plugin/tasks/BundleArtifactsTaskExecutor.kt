package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Project
import org.gradle.api.GradleException
import org.gradle.api.publish.PublishingExtension
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Executes bundle creation logic for the bundleArtifacts task.
 * 
 * Follows Single Responsibility Principle - handles only bundle creation.
 */
class BundleArtifactsTaskExecutor(
    private val project: Project,
    private val config: CentralPublisherConfig
) {
    
    fun execute() {
        project.logger.quiet("📦 Creating deployment bundle...")
        
        try {
            // Check if publishing extension is available
            val publishingExtension = project.extensions.findByType(PublishingExtension::class.java)
                ?: throw GradleException("maven-publish plugin not applied. Please apply 'maven-publish' plugin.")
            
            // Look for local repository artifacts - check current project and all submodules
            val localRepoDir = findLocalRepositoryWithArtifacts()
                ?: throw GradleException("No artifacts found to bundle. Please run publishToLocalRepo task first.")
            
            // Create bundle directory
            val bundleDir = File(project.buildDir, "central-portal")
            bundleDir.mkdirs()
            
            // Create bundle zip file
            val bundleFile = File(bundleDir, "${project.name}-${project.version}-bundle.zip")
            
            project.logger.quiet("📁 Bundling artifacts from: ${localRepoDir.absolutePath}")
            project.logger.quiet("📦 Creating bundle: ${bundleFile.absolutePath}")
            
            createBundleZip(localRepoDir, bundleFile)
            
            project.logger.quiet("✅ Bundle created successfully!")
            project.logger.quiet("📊 Bundle size: ${bundleFile.length()} bytes")
            project.logger.quiet("💡 Next step: Run './gradlew publishToCentral' to upload to Maven Central")
            
        } catch (e: Exception) {
            if (e is GradleException) {
                throw e
            }
            project.logger.error("❌ Bundle creation failed: ${e.message}")
            throw GradleException("Bundle creation error: ${e.message}", e)
        }
    }
    
    /**
     * Finds local repository directory with artifacts.
     * In multi-module projects, all artifacts are published to the root project's repository.
     */
    private fun findLocalRepositoryWithArtifacts(): File? {
        // Always check root project's repository first - this is where artifacts are published
        val rootProject = project.rootProject
        val signedRepoDir = File(rootProject.buildDir, "maven-repo")
        val unsignedRepoDir = File(rootProject.buildDir, "repo")
        
        project.logger.quiet("🔍 Looking for artifacts in root project repository: ${signedRepoDir.absolutePath}")
        
        when {
            signedRepoDir.exists() && signedRepoDir.listFiles()?.isNotEmpty() == true -> {
                project.logger.quiet("📝 Found signed artifacts in root project at maven-repo")
                return signedRepoDir
            }
            unsignedRepoDir.exists() && unsignedRepoDir.listFiles()?.isNotEmpty() == true -> {
                project.logger.warn("⚠️ Found unsigned artifacts in root project at repo")
                project.logger.warn("⚠️ Maven Central requires signed artifacts")
                project.logger.warn("⚠️ Please configure signing credentials and re-run the build")
                project.logger.warn("💡 See SIGNING_SETUP.md for signing configuration instructions")
                return unsignedRepoDir
            }
            else -> {
                project.logger.quiet("🔍 No artifacts found in root project repository")
                project.logger.quiet("📂 Checked directories:")
                project.logger.quiet("   - ${signedRepoDir.absolutePath} (exists: ${signedRepoDir.exists()})")
                project.logger.quiet("   - ${unsignedRepoDir.absolutePath} (exists: ${unsignedRepoDir.exists()})")
                return null
            }
        }
    }
    
    private fun createBundleZip(sourceDir: File, zipFile: File) {
        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = sourceDir.toPath().relativize(file.toPath()).toString()
                    zip.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { input ->
                        input.copyTo(zip)
                    }
                    zip.closeEntry()
                }
            }
        }
    }
}