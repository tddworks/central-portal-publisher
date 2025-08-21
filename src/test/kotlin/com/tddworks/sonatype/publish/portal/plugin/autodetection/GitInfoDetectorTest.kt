package com.tddworks.sonatype.publish.portal.plugin.autodetection

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GitInfoDetectorTest {
    
    private lateinit var project: Project
    private lateinit var detector: GitInfoDetector
    
    @TempDir
    lateinit var tempDir: File
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        detector = GitInfoDetector()
    }
    
    @Test
    fun `should return null when no git directory exists`() {
        // Given - no .git directory
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNull()
    }
    
    @Test
    fun `should detect github ssh remote url`() {
        // Given
        createGitConfig("""
            [remote "origin"]
                url = git@github.com:tddworks/central-portal-publisher.git
                fetch = +refs/heads/*:refs/remotes/origin/*
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.url).isEqualTo("https://github.com/tddworks/central-portal-publisher")
        assertThat(result.config.projectInfo.scm.url).isEqualTo("https://github.com/tddworks/central-portal-publisher")
        assertThat(result.config.projectInfo.scm.connection).isEqualTo("scm:git:https://github.com/tddworks/central-portal-publisher.git")
        assertThat(result.config.projectInfo.scm.developerConnection).isEqualTo("scm:git:git@github.com:tddworks/central-portal-publisher.git")
        
        // Check detected values
        assertThat(result.detectedValues).hasSize(4)
        assertThat(result.detectedValues["projectInfo.url"]?.confidence).isEqualTo(Confidence.HIGH)
        assertThat(result.detectedValues["projectInfo.url"]?.source).isEqualTo(".git/config")
    }
    
    @Test
    fun `should detect github https remote url`() {
        // Given
        createGitConfig("""
            [remote "origin"]
                url = https://github.com/tddworks/central-portal-publisher.git
                fetch = +refs/heads/*:refs/remotes/origin/*
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.url).isEqualTo("https://github.com/tddworks/central-portal-publisher")
        assertThat(result.config.projectInfo.scm.connection).isEqualTo("scm:git:https://github.com/tddworks/central-portal-publisher.git")
        assertThat(result.config.projectInfo.scm.developerConnection).isEqualTo("scm:git:https://github.com/tddworks/central-portal-publisher.git")
    }
    
    @Test
    fun `should detect gitlab ssh remote url`() {
        // Given
        createGitConfig("""
            [remote "origin"]
                url = git@gitlab.com:mygroup/myproject.git
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.url).isEqualTo("https://gitlab.com/mygroup/myproject")
        assertThat(result.config.projectInfo.scm.url).isEqualTo("https://gitlab.com/mygroup/myproject")
        assertThat(result.config.projectInfo.scm.connection).isEqualTo("scm:git:https://gitlab.com/mygroup/myproject.git")
        assertThat(result.config.projectInfo.scm.developerConnection).isEqualTo("scm:git:git@gitlab.com:mygroup/myproject.git")
    }
    
    @Test
    fun `should handle generic ssh format`() {
        // Given
        createGitConfig("""
            [remote "origin"]
                url = git@example.com:user/repo.git
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.url).isEqualTo("https://example.com/user/repo")
        assertThat(result.config.projectInfo.scm.connection).isEqualTo("scm:git:https://example.com/user/repo.git")
        assertThat(result.config.projectInfo.scm.developerConnection).isEqualTo("scm:git:git@example.com:user/repo.git")
    }
    
    @Test
    fun `should detect developer info from git config`() {
        // Given
        createGitConfig("""
            [user]
                name = John Developer
                email = john@example.com
            [remote "origin"]
                url = https://github.com/john/project.git
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        
        // Check detected values (developer info is recorded but not set in config yet)
        assertThat(result!!.detectedValues["projectInfo.developer.name"]?.value).isEqualTo("John Developer")
        assertThat(result.detectedValues["projectInfo.developer.email"]?.value).isEqualTo("john@example.com")
        assertThat(result.detectedValues["projectInfo.developer.name"]?.confidence).isEqualTo(Confidence.HIGH)
    }
    
    @Test
    fun `should fall back to any remote when origin not found`() {
        // Given
        createGitConfig("""
            [remote "upstream"]
                url = https://github.com/upstream/project.git
            [remote "fork"]
                url = git@github.com:me/project.git
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        // Should detect from first available remote (upstream in this case)
        assertThat(result!!.config.projectInfo.url).contains("github.com")
    }
    
    @Test
    fun `should add warning when no remote url found`() {
        // Given - git config with no remotes
        createGitConfig("""
            [user]
                name = Test User
                email = test@example.com
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.warnings).contains("No suitable Git remote URL found for SCM configuration")
        
        // But should still detect developer info
        assertThat(result.detectedValues).containsKeys("projectInfo.developer.name", "projectInfo.developer.email")
    }
    
    @Test
    fun `should handle malformed git config gracefully`() {
        // Given - malformed config
        createGitConfig("""
            [remote "origin"
                url = https://github.com/test/project.git
            invalid line without equals
            [incomplete section
        """.trimIndent())
        
        // When
        val result = detector.detect(project)
        
        // Then - should not crash, may or may not detect anything
        assertThat(result).satisfiesAnyOf(
            { assertThat(it).isNull() },
            { assertThat(it!!.detectedValues).isEmpty() }
        )
    }
    
    @Test
    fun `should find git directory in parent folder`() {
        // Given - .git in parent directory
        val parentGitDir = File(tempDir, ".git")
        parentGitDir.mkdirs()
        File(parentGitDir, "config").writeText("""
            [remote "origin"]
                url = https://github.com/parent/project.git
        """.trimIndent())
        
        // Project in subdirectory
        val subDir = File(tempDir, "subproject")
        subDir.mkdirs()
        project = ProjectBuilder.builder().withProjectDir(subDir).build()
        
        // When
        val result = detector.detect(project)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result!!.config.projectInfo.url).isEqualTo("https://github.com/parent/project")
    }
    
    @Test
    fun `should have correct detector name and default enablement`() {
        // When/Then
        assertThat(detector.name).isEqualTo("GitInfoDetector")
        assertThat(detector.enabledByDefault).isTrue()
    }
    
    private fun createGitConfig(content: String) {
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()
        
        val configFile = File(gitDir, "config")
        configFile.writeText(content)
    }
}