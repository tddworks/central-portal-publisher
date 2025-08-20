package com.tddworks.sonatype.publish.portal.plugin.dsl

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CentralPublisherPluginIntegrationTest {
    
    private lateinit var project: Project
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
    }
    
    @Test
    fun `should register centralPublisher extension when plugin is applied`() {
        // When - Apply the plugin (this will fail until we create the plugin)
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // Then - Extension should be available
        val extension = project.extensions.findByType(CentralPublisherExtension::class.java)
        assertThat(extension).isNotNull()
        
        val extensionByName = project.extensions.findByName("centralPublisher")
        assertThat(extensionByName).isNotNull()
        assertThat(extensionByName).isInstanceOf(CentralPublisherExtension::class.java)
    }
    
    @Test
    fun `should integrate DSL with configuration system`() {
        // Given - Apply plugin
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure via DSL
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
            
            projectInfo {
                name = "integration-test"
                description = "Integration test project"
                url = "https://github.com/test/integration"
                
                scm {
                    url = "https://github.com/test/integration"
                    connection = "scm:git:https://github.com/test/integration.git"
                }
                
                license {
                    name = "Apache License 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                }
                
                developer {
                    name = "Test Developer"
                    email = "test@example.com"
                }
            }
            
            signing {
                keyId = "ABCD1234"
                password = "signing-pass"
                secretKeyRingFile = "/tmp/test-keyring.gpg"
            }
            
            publishing {
                autoPublish = false
                dryRun = true
            }
        }
        
        // Then - Configuration should be properly set
        val finalConfig = (project.extensions.getByType(CentralPublisherExtension::class.java)).build()
        
        assertThat(finalConfig.credentials.username).isEqualTo("test-user")
        assertThat(finalConfig.credentials.password).isEqualTo("test-token")
        assertThat(finalConfig.projectInfo.name).isEqualTo("integration-test")
        assertThat(finalConfig.projectInfo.description).isEqualTo("Integration test project")
        assertThat(finalConfig.projectInfo.scm.connection).isEqualTo("scm:git:https://github.com/test/integration.git")
        assertThat(finalConfig.projectInfo.license.name).isEqualTo("Apache License 2.0")
        assertThat(finalConfig.projectInfo.developers).hasSize(1)
        assertThat(finalConfig.projectInfo.developers[0].name).isEqualTo("Test Developer")
        assertThat(finalConfig.signing.keyId).isEqualTo("ABCD1234")
        assertThat(finalConfig.publishing.autoPublish).isFalse()
        assertThat(finalConfig.publishing.dryRun).isTrue()
    }
    
    @Test
    fun `should support auto-detection integration with DSL`() {
        // Given - Apply plugin
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure with minimal DSL (should use auto-detection for missing values)
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "minimal-user"
                password = "minimal-token"
            }
            // No projectInfo - should be auto-detected
        }
        
        // Then - Should have both DSL config and auto-detected values
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()
        
        assertThat(config.credentials.username).isEqualTo("minimal-user")
        assertThat(config.credentials.password).isEqualTo("minimal-token")
        
        // Auto-detected values might be present (project name from directory, etc.)
        // The exact auto-detected values depend on the test environment
        assertThat(config.projectInfo.name).isNotEmpty() // Should have some project name
    }
    
    @Test
    fun `should validate DSL configuration when building`() {
        // Given - Apply plugin  
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure with invalid values
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "" // Invalid - empty
                password = "some-password"
            }
        }
        
        // Then - Should have validation errors when building config
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()
        
        // Validation should be run through the ValidationEngine
        val validationEngine = com.tddworks.sonatype.publish.portal.plugin.validation.ValidationEngine()
        val validationResult = validationEngine.validate(config)
        
        assertThat(validationResult.isValid).isFalse()
        assertThat(validationResult.getErrors()).isNotEmpty()
        assertThat(validationResult.getErrors()).anySatisfy { error ->
            assertThat(error.field).isEqualTo("credentials.username")
            assertThat(error.code).startsWith("REQ-")
        }
    }
}