package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.ProjectInfoConfig
import org.assertj.core.api.Assertions
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PublicationProviderRegistryTest {

    private lateinit var project: Project
    private lateinit var config: CentralPublisherConfig

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        config = CentralPublisherConfig(
            projectInfo = ProjectInfoConfig(
                name = "test-registry-library",
                description = "A test library for registry",
                url = "https://github.com/example/test-registry-library"
            )
        )
    }

    @Test
    fun `should apply all applicable publication providers`() {
        // Given: A project with Java and maven-publish plugins (opt-in behavior)
        project.plugins.apply("java")
        project.plugins.apply("maven-publish")

        val registry = PublicationProviderRegistry()

        // When: Configure publications via registry
        registry.configurePublications(project, config)

        // Then: Publications should be configured by applicable providers
        Assertions.assertThat(project.plugins.hasPlugin("maven-publish")).isEqualTo(true)
        val publishing = project.extensions.getByType<PublishingExtension>()
        
        // Should create at least one publication (may create sources jar as second publication)
        Assertions.assertThat(publishing.publications.size).isGreaterThanOrEqualTo(1)
        // Should have a maven publication configured
        Assertions.assertThat(publishing.publications.findByName("maven")).isNotNull
    }

    @Test
    fun `should handle projects with no applicable plugins gracefully`() {
        // Given: A project with no JVM or KMP plugins
        val registry = PublicationProviderRegistry()

        // When: Configure publications via registry
        registry.configurePublications(project, config)

        // Then: Should complete without error and NOT apply maven-publish (opt-in behavior)
        Assertions.assertThat(project.plugins.hasPlugin("maven-publish")).isEqualTo(false)
        // No publishing extension should exist since no maven-publish plugin was applied
    }

    @Test
    fun `should work with pre-existing maven-publish plugin`() {
        // Given: A project with maven-publish already applied
        project.plugins.apply("maven-publish")
        project.plugins.apply("java")

        val registry = PublicationProviderRegistry()

        // When: Configure publications via registry
        registry.configurePublications(project, config)

        // Then: Should not fail and publications should be configured
        Assertions.assertThat(project.plugins.hasPlugin("maven-publish")).isEqualTo(true)
        val publishing = project.extensions.getByType<PublishingExtension>()
        // Should create at least one publication (may create sources jar as second publication)
        Assertions.assertThat(publishing.publications.size).isGreaterThanOrEqualTo(1)
        // Should have a maven publication configured
        Assertions.assertThat(publishing.publications.findByName("maven")).isNotNull
    }
}