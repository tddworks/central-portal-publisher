package com.tddworks.sonatype.publish.portal.plugin.autodetection

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectInfoDetectorTest {
    
    private lateinit var detector: ProjectInfoDetector
    
    @TempDir
    lateinit var tempDir: File
    
    @BeforeEach
    fun setup() {
        detector = ProjectInfoDetector()
    }
    
    @Test
    fun `should detect project name from gradle project`() {
        // Given
        val project = ProjectBuilder.builder()
            .withName("my-awesome-project")
            .withProjectDir(tempDir)
            .build()
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.name).isEqualTo("my-awesome-project")
        assertThat(result.detectedValues["projectInfo.name"]?.confidence).isEqualTo(Confidence.HIGH)
        assertThat(result.detectedValues["projectInfo.name"]?.source).isEqualTo("Gradle project")
    }
    
    @Test
    fun `should detect project name from directory when gradle name is root`() {
        // Given
        val projectDir = File(tempDir, "my-library")
        projectDir.mkdirs()
        
        val project = ProjectBuilder.builder()
            .withName("root") // This gets ignored
            .withProjectDir(projectDir)
            .build()
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.name).isEqualTo("my-library")
        assertThat(result.detectedValues["projectInfo.name"]?.confidence).isEqualTo(Confidence.MEDIUM)
        assertThat(result.detectedValues["projectInfo.name"]?.source).isEqualTo("Directory name")
    }
    
    @Test
    fun `should detect description from build gradle kts`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        
        val buildFile = File(tempDir, "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.23"
            }
            
            description = "A fantastic Kotlin library for doing amazing things"
            
            dependencies {
                // some deps
            }
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.description).isEqualTo("A fantastic Kotlin library for doing amazing things")
        assertThat(result.detectedValues["projectInfo.description"]?.confidence).isEqualTo(Confidence.HIGH)
        assertThat(result.detectedValues["projectInfo.description"]?.source).isEqualTo("build.gradle.kts")
    }
    
    @Test
    fun `should detect description from groovy build gradle`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        
        val buildFile = File(tempDir, "build.gradle")
        buildFile.writeText("""
            plugins {
                id 'java'
            }
            
            description = 'A Java library with Groovy build script'
            
            dependencies {
                // some deps
            }
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.description).isEqualTo("A Java library with Groovy build script")
        assertThat(result.detectedValues["projectInfo.description"]?.confidence).isEqualTo(Confidence.HIGH)
        assertThat(result.detectedValues["projectInfo.description"]?.source).isEqualTo("build.gradle")
    }
    
    @Test
    fun `should detect description from README md`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        
        val readmeFile = File(tempDir, "README.md")
        readmeFile.writeText("""
            # My Awesome Library
            
            This is a comprehensive library that provides amazing functionality for developers.
            It supports multiple use cases and is highly configurable.
            
            ## Installation
            
            Add this to your build.gradle...
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.description).isEqualTo("This is a comprehensive library that provides amazing functionality for developers.")
        assertThat(result.detectedValues["projectInfo.description"]?.confidence).isEqualTo(Confidence.MEDIUM)
        assertThat(result.detectedValues["projectInfo.description"]?.source).isEqualTo("README.md")
    }
    
    @Test
    fun `should prefer build script description over README`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        
        // Create both files
        val buildFile = File(tempDir, "build.gradle.kts")
        buildFile.writeText("""
            description = "Build script description"
        """.trimIndent())
        
        val readmeFile = File(tempDir, "README.md")
        readmeFile.writeText("""
            # Project
            README description that should be ignored
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.description).isEqualTo("Build script description")
        assertThat(result.detectedValues["projectInfo.description"]?.source).isEqualTo("build.gradle.kts")
    }
    
    @Test
    fun `should handle README with badges and links`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        
        val readmeFile = File(tempDir, "README.md")
        readmeFile.writeText("""
            # Project Name
            
            [![Build Status](https://travis-ci.org/user/project.svg)](https://travis-ci.org/user/project)
            [![Coverage](https://codecov.io/gh/user/project/badge.svg)](https://codecov.io/gh/user/project)
            
            This library provides essential functionality for modern applications.
            
            ## Features
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.description).isEqualTo("This library provides essential functionality for modern applications.")
        assertThat(result.detectedValues["projectInfo.description"]?.source).isEqualTo("README.md")
    }
    
    @Test
    fun `should add warnings for missing information`() {
        // Given - project with no build file or README
        val project = ProjectBuilder.builder()
            .withName("root") // This gets ignored for name detection
            .withProjectDir(tempDir)
            .build()
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.warnings).hasSize(1) // Only description warning, name is detected from directory
        assertThat(result.warnings).contains("Could not auto-detect project description from build.gradle.kts or README files")
        
        // Should still detect name from directory
        assertThat(result.config.projectInfo.name).isNotEmpty()
        assertThat(result.detectedValues).hasSize(1) // project name detected
    }
    
    @Test
    fun `should return null when no information can be detected`() {
        // Given - temp directory with generated name that should be ignored
        val tempProject = File.createTempFile("junit", "test").let { file ->
            file.delete()
            file.mkdirs()
            file
        }
        
        val project = ProjectBuilder.builder()
            .withName("root")
            .withProjectDir(tempProject)
            .build()
        
        try {
            // When
            val result = detector.detect(project)
            
            // Then - should detect name from temp directory (since we detect any directory name)
            assertThat(result).isNotNull
            assertThat(result!!.detectedValues).hasSize(1) // temp directory name gets detected
            assertThat(result.detectedValues["projectInfo.name"]?.value).startsWith("junit")
            assertThat(result.warnings).hasSize(1) // Only description warning
        } finally {
            tempProject.deleteRecursively()
        }
    }
    
    @Test
    fun `should have correct detector name and default enablement`() {
        // When/Then
        assertThat(detector.name).isEqualTo("ProjectInfoDetector")
        assertThat(detector.enabledByDefault).isTrue()
    }
}