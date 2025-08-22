package com.tddworks.sonatype.publish.portal.plugin.wizard

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfigBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * Enhanced mock prompt system for testing refactored wizard
 */
class MockPromptSystem : PromptSystem {
    private val responses = mutableListOf<String>()
    private val confirmResponses = mutableListOf<Boolean>()
    private var currentIndex = 0
    private var currentConfirmIndex = 0

    val prompts = mutableListOf<String>()
    var lastPrompt: String = ""
    var allPrompts: String = "" // Concatenates all prompts for easier testing
    val displayMessages = mutableListOf<String>() // Track display() calls

    fun addResponse(response: String) {
        responses.add(response)
    }

    fun addConfirmResponse(response: Boolean) {
        confirmResponses.add(response)
    }

    override fun prompt(message: String): String {
        lastPrompt = message
        prompts.add(message)
        allPrompts += message + "\n"
        return if (currentIndex < responses.size) {
            responses[currentIndex++]
        } else {
            "" // Default empty response
        }
    }

    override fun promptWithDefault(message: String, defaultValue: String): String {
        val response = prompt(message)
        return response.ifEmpty { defaultValue }
    }

    override fun confirm(message: String): Boolean {
        // Store the confirm message but don't overwrite lastPrompt from prompt() calls
        prompts.add(message)
        allPrompts += message + "\n"

        // First try explicit confirm responses
        if (currentConfirmIndex < confirmResponses.size) {
            return confirmResponses[currentConfirmIndex++]
        }

        // Fall back to converting string responses to boolean (for compatibility)
        val response = if (currentIndex < responses.size) {
            responses[currentIndex++]
        } else {
            ""
        }
        return toBooleanResponse(response)
    }

    override fun select(message: String, options: List<String>): String {
        val response = prompt(message)
        return if (response.isNotEmpty() && options.contains(response)) {
            response
        } else {
            options.first() // Default to first option
        }
    }

    override fun display(message: String) {
        displayMessages.add(message)
    }

    private fun toBooleanResponse(response: String): Boolean {
        return response.lowercase() in listOf("y", "yes", "true")
    }
}

/**
 * Test project builder utility
 */
object TestProjectBuilder {
    fun createProject(name: String = "test-project"): Project {
        return ProjectBuilder.builder()
            .withName(name)
            .build()
    }
}

/**
 * Test configuration builder utility
 */
object TestConfigBuilder {
    fun createConfig(): CentralPublisherConfig {
        return CentralPublisherConfigBuilder()
            .credentials {
                username = "test-user"
                password = "test-password"
            }
            .projectInfo {
                name = "test-project"
                description = "Test project description"
                url = "https://github.com/test/test-project"
                license {
                    name = "MIT"
                    url = "https://opensource.org/licenses/MIT"
                }
                developer {
                    id = "testdev"
                    name = "Test Developer"
                    email = "test@example.com"
                }
            }
            .signing {
                keyId = "test-key"
                password = "test-key-password"
            }
            .publishing {
                autoPublish = false
                aggregation = true
            }
            .build()
    }
}