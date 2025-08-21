package com.tddworks.sonatype.publish.portal.plugin.dryrun

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the DryRunEngine that simulates publishing operations.
 * 
 * The DryRunEngine should validate configuration and simulate all publishing
 * steps without actually performing any network operations or file uploads.
 */
class DryRunEngineTest {
    
    private lateinit var project: Project
    private lateinit var dryRunEngine: DryRunEngine
    
    @TempDir
    lateinit var tempDir: File
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        dryRunEngine = DryRunEngine()
    }
    
    @Test
    fun `should create DryRunEngine with default configuration`() {
        // When
        val engine = DryRunEngine()
        
        // Then
        assertThat(engine).isNotNull()
        assertThat(engine.isDryRunEnabled).isFalse() // Default disabled
    }
    
    @Test
    fun `should enable dry run mode`() {
        // When
        val engine = DryRunEngine(dryRunEnabled = true)
        
        // Then
        assertThat(engine.isDryRunEnabled).isTrue()
    }
    
    @Test
    fun `should validate configuration in dry run mode`() {
        // Given
        val validConfig = createValidConfig()
        val engine = DryRunEngine(dryRunEnabled = true)
        
        // When
        val result = engine.simulatePublishing(project, validConfig)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.validationErrors).isEmpty()
        assertThat(result.simulatedSteps).isNotEmpty()
        assertThat(result.wouldPublish).isTrue()
    }
    
    @Test
    fun `should detect validation errors in dry run mode`() {
        // Given - Invalid configuration (missing credentials)
        val invalidConfig = CentralPublisherConfig(
            credentials = CredentialsConfig("", ""), // Empty credentials
            projectInfo = ProjectInfoConfig(name = "test-project")
        )
        val engine = DryRunEngine(dryRunEnabled = true)
        
        // When
        val result = engine.simulatePublishing(project, invalidConfig)
        
        // Then
        assertThat(result.isSuccess).isFalse()
        assertThat(result.validationErrors).isNotEmpty()
        assertThat(result.wouldPublish).isFalse()
        assertThat(result.simulatedSteps).isNotEmpty() // Should still show what it tried to do
    }
    
    @Test
    fun `should simulate all publishing steps without side effects`() {
        // Given
        val config = createValidConfig()
        val engine = DryRunEngine(dryRunEnabled = true)
        
        // When
        val result = engine.simulatePublishing(project, config)
        
        // Then - Should simulate all major steps
        val stepNames = result.simulatedSteps.map { it.stepName }
        assertThat(stepNames).contains(
            "Configuration Validation",
            "Artifact Preparation", 
            "POM Generation",
            "GPG Signing Simulation",
            "Publishing Simulation"
        )
        
        // Should contain either aggregated or individual upload simulation
        assertThat(stepNames).containsAnyOf(
            "Aggregated Upload Simulation",
            "Individual Upload Simulation"
        )
        
        // Each step should be marked as simulated
        result.simulatedSteps.forEach { step ->
            assertThat(step.isSimulated).isTrue()
            assertThat(step.description).contains("DRY RUN")
        }
    }
    
    @Test
    fun `should provide detailed simulation report`() {
        // Given
        val config = createValidConfig()
        val engine = DryRunEngine(dryRunEnabled = true)
        
        // When  
        val result = engine.simulatePublishing(project, config)
        
        // Then
        assertThat(result.simulatedSteps).hasSizeGreaterThan(3)
        
        result.simulatedSteps.forEach { step ->
            assertThat(step.stepName).isNotEmpty()
            assertThat(step.description).isNotEmpty()
            assertThat(step.duration).isGreaterThanOrEqualTo(0)
        }
        
        // Should have summary information
        assertThat(result.totalSteps).isEqualTo(result.simulatedSteps.size)
        assertThat(result.totalDuration).isGreaterThanOrEqualTo(0)
    }
    
    @Test
    fun `should simulate different outcomes based on configuration`() {
        // Given - Config with autoPublish enabled
        val autoPublishConfig = createValidConfig().copy(
            publishing = PublishingConfig(autoPublish = true)
        )
        val manualConfig = createValidConfig().copy(
            publishing = PublishingConfig(autoPublish = false)
        )
        
        val engine = DryRunEngine(dryRunEnabled = true)
        
        // When
        val autoResult = engine.simulatePublishing(project, autoPublishConfig)
        val manualResult = engine.simulatePublishing(project, manualConfig)
        
        // Then - Auto-publish should have more steps
        assertThat(autoResult.simulatedSteps.map { it.stepName })
            .contains("Auto-Publish Simulation")
        
        assertThat(manualResult.simulatedSteps.map { it.stepName })
            .doesNotContain("Auto-Publish Simulation")
            .contains("Manual Review Required")
    }
    
    @Test
    fun `should handle aggregation vs individual publishing simulation`() {
        // Given - Different aggregation settings
        val aggregatedConfig = createValidConfig().copy(
            publishing = PublishingConfig(aggregation = true)
        )
        val individualConfig = createValidConfig().copy(
            publishing = PublishingConfig(aggregation = false)
        )
        
        val engine = DryRunEngine(dryRunEnabled = true)
        
        // When
        val aggregatedResult = engine.simulatePublishing(project, aggregatedConfig)
        val individualResult = engine.simulatePublishing(project, individualConfig)
        
        // Then - Should simulate different upload strategies
        val aggregatedSteps = aggregatedResult.simulatedSteps.map { it.stepName }
        val individualSteps = individualResult.simulatedSteps.map { it.stepName }
        
        assertThat(aggregatedSteps).contains("Aggregated Upload Simulation")
        assertThat(individualSteps).contains("Individual Upload Simulation")
    }
    
    @Test
    fun `should not perform actual operations when dry run is disabled but dryRun config is true`() {
        // Given - Engine disabled but config has dryRun = true
        val config = createValidConfig().copy(
            publishing = PublishingConfig(dryRun = true)
        )
        val engine = DryRunEngine(dryRunEnabled = false)
        
        // When
        val result = engine.simulatePublishing(project, config)
        
        // Then - Should still respect config dryRun setting
        assertThat(result.isDryRun).isTrue()
        assertThat(result.simulatedSteps).isNotEmpty()
    }
    
    private fun createValidConfig(): CentralPublisherConfig {
        return CentralPublisherConfig(
            credentials = CredentialsConfig(
                username = "test-user",
                password = "test-token"
            ),
            projectInfo = ProjectInfoConfig(
                name = "test-project",
                description = "A test project",
                url = "https://github.com/test/project",
                scm = ScmConfig(
                    url = "https://github.com/test/project",
                    connection = "scm:git:https://github.com/test/project.git"
                ),
                license = LicenseConfig(
                    name = "Apache License 2.0",
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                ),
                developers = listOf(
                    DeveloperConfig(
                        name = "Test Developer",
                        email = "test@example.com"
                    )
                )
            ),
            signing = SigningConfig(
                keyId = "12345678",
                password = "signing-password",
                secretKeyRingFile = "/path/to/keyring.gpg"
            ),
            publishing = PublishingConfig(
                autoPublish = false,
                aggregation = true,
                dryRun = false
            )
        )
    }
}