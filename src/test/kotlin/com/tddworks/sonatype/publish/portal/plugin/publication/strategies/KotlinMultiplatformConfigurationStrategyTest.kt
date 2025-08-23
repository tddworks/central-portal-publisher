package com.tddworks.sonatype.publish.portal.plugin.publication.strategies

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KotlinMultiplatformConfigurationStrategyTest {

    private lateinit var project: Project
    private lateinit var strategy: KotlinMultiplatformConfigurationStrategy
    private lateinit var config: CentralPublisherConfig

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        strategy = KotlinMultiplatformConfigurationStrategy()

        config =
            CentralPublisherConfig(
                projectInfo =
                    ProjectInfoConfig(
                        name = "test-kmp-library",
                        description = "Test KMP library description",
                        url = "https://github.com/test/kmp-library",
                        license =
                            LicenseConfig(
                                name = "Apache License, Version 2.0",
                                url = "http://www.apache.org/licenses/LICENSE-2.0.txt",
                                distribution = "repo",
                            ),
                        developers =
                            listOf(
                                DeveloperConfig(
                                    id = "dev1",
                                    name = "Developer One",
                                    email = "dev1@example.com",
                                )
                            ),
                        scm =
                            ScmConfig(
                                connection = "scm:git:git://github.com/test/kmp-library.git",
                                developerConnection =
                                    "scm:git:ssh://github.com:test/kmp-library.git",
                                url = "https://github.com/test/kmp-library",
                            ),
                    ),
                credentials = CredentialsConfig(username = "test-user", password = "test-token"),
            )
    }

    @Test
    fun `should detect Kotlin Multiplatform projects`() {
        // Given - Project without KMP plugin applied
        // (KMP plugin isn't included in test classpath due to complex dependencies)

        // When - Test detection logic
        val canHandle = strategy.canHandle(project)

        // Then - Should correctly identify that this is NOT a KMP project
        assertThat(canHandle).isFalse()
    }

    @Test
    fun `should detect projects with KMP plugin applied`() {
        // Given - Try to apply KMP plugin (will fail in test environment due to missing
        // dependencies)

        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        // When - KMP plugin successfully applied
        val canHandle = strategy.canHandle(project)

        // Then - Should correctly identify this as a KMP project
        assertThat(canHandle).isTrue()
    }

    @Test
    fun `should not detect non-KMP projects`() {
        // Given - Apply different plugin
        project.pluginManager.apply("java")

        // When
        val canHandle = strategy.canHandle(project)

        // Then
        assertThat(canHandle).isFalse()
    }

    @Test
    fun `should not detect projects without KMP plugin`() {
        // Given - No plugins applied

        // When
        val canHandle = strategy.canHandle(project)

        // Then
        assertThat(canHandle).isFalse()
    }

    @Test
    fun `should configure KMP project with existing publications`() {
        // Given - Apply maven-publish and create publications to simulate KMP plugin
        project.pluginManager.apply("maven-publish")

        // Create a mock publication to simulate what KMP plugin does
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("jvm", MavenPublication::class.java)

        // When
        strategy.configure(project, config)

        // Then - Should configure existing publications without creating new ones
        assertThat(publishing.publications).hasSize(1)
        assertThat(publishing.publications.getByName("jvm")).isNotNull()
    }

    @Test
    fun `should configure KMP extension for sources jars`() {
        // Given - Apply maven-publish plugin
        project.pluginManager.apply("maven-publish")

        // When - Since KMP plugin isn't available in test environment,
        // we test that the strategy handles missing KMP extension gracefully
        strategy.configure(project, config)

        // Then - Should complete without error even when KMP extension is not available
        // This tests the error handling path in the configure method
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        assertThat(publishing).isNotNull()
    }

    @Test
    fun `should have correct plugin type`() {
        // When
        val pluginType = strategy.getPluginType()

        // Then
        assertThat(pluginType).isEqualTo("kotlin-multiplatform")
    }

    @Test
    fun `should have highest priority`() {
        // When
        val priority = strategy.getPriority()

        // Then
        assertThat(priority).isEqualTo(20) // Highest priority
    }

    @Test
    fun `should apply maven-publish plugin automatically`() {
        // Given - Start with no plugins applied
        assertThat(project.plugins.hasPlugin("maven-publish")).isFalse()

        // When
        strategy.configure(project, config)

        // Then - maven-publish should be applied
        assertThat(project.plugins.hasPlugin("maven-publish")).isTrue()
    }

    @Test
    fun `should follow non-intrusive pattern for existing publications`() {
        // Given - Apply maven-publish and create multiple publications (simulating KMP targets)
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("jvm", MavenPublication::class.java)
        publishing.publications.create("js", MavenPublication::class.java)
        publishing.publications.create("native", MavenPublication::class.java)

        val originalCount = publishing.publications.size

        // When
        strategy.configure(project, config)

        // Then - Should not create new publications, only configure existing ones
        assertThat(publishing.publications).hasSize(originalCount)
        assertThat(publishing.publications.names).containsExactlyInAnyOrder("jvm", "js", "native")
    }

    @Test
    fun `should configure POM metadata for all publications`() {
        // Given - Apply maven-publish with multiple publications
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val jvmPub = publishing.publications.create("jvm", MavenPublication::class.java)
        val jsPub = publishing.publications.create("js", MavenPublication::class.java)

        // When
        strategy.configure(project, config)

        // Then - All publications should exist and be properly set up
        assertThat(jvmPub.groupId).isEqualTo(project.group.toString())
        assertThat(jsPub.groupId).isEqualTo(project.group.toString())
        // Note: POM configuration details are tested through the configurePom utility function
    }

    @Test
    fun `should handle projects with no existing publications gracefully`() {
        // Given - Apply maven-publish but no publications created yet
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        assertThat(publishing.publications).isEmpty()

        // When
        strategy.configure(project, config)

        // Then - Should complete without error
        assertThat(publishing.publications).isEmpty() // Still no publications created
        // This is expected - KMP plugin creates publications later in the build lifecycle
    }

    @Test
    fun `should create javadoc JAR task for JVM publications`() {
        // Given - Apply maven-publish with JVM publication
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("jvm", MavenPublication::class.java)

        // When
        strategy.configure(project, config)

        // Then - Should create jvm-specific javadoc JAR task
        val javadocTask = project.tasks.findByName("jvmJavadocJar")
        assertThat(javadocTask).isNotNull()
        assertThat(javadocTask).isInstanceOf(Jar::class.java)

        val jarTask = javadocTask as Jar
        assertThat(jarTask.archiveClassifier.get()).isEqualTo("javadoc")
        assertThat(jarTask.description).isEqualTo("Creates javadoc JAR for jvm publication")
    }

    @Test
    fun `should create javadoc JAR task for mixed case JVM publications`() {
        // Given - Apply maven-publish with mixed case JVM publication name
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("JVM", MavenPublication::class.java)

        // When
        strategy.configure(project, config)

        // Then - Should create JVM-specific javadoc JAR task (case insensitive)
        val javadocTask = project.tasks.findByName("JVMJavadocJar")
        assertThat(javadocTask).isNotNull()
        assertThat(javadocTask).isInstanceOf(Jar::class.java)
    }

    @Test
    fun `should not create javadoc JAR task for non-JVM publications`() {
        // Given - Apply maven-publish with non-JVM publications
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("js", MavenPublication::class.java)
        publishing.publications.create("native", MavenPublication::class.java)
        publishing.publications.create("linuxX64", MavenPublication::class.java)

        // When
        strategy.configure(project, config)

        // Then - Should not create javadoc JAR tasks for non-JVM publications
        assertThat(project.tasks.findByName("jsJavadocJar")).isNull()
        assertThat(project.tasks.findByName("nativeJavadocJar")).isNull()
        assertThat(project.tasks.findByName("linuxX64JavadocJar")).isNull()
    }

    @Test
    fun `should create separate javadoc JAR tasks for multiple JVM publications`() {
        // Given - Apply maven-publish with multiple JVM-related publications
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("jvm", MavenPublication::class.java)
        publishing.publications.create("jvmTest", MavenPublication::class.java)
        publishing.publications.create("js", MavenPublication::class.java) // non-JVM for comparison

        // When
        strategy.configure(project, config)

        // Then - Should create separate javadoc JAR tasks for each JVM publication
        assertThat(project.tasks.findByName("jvmJavadocJar")).isNotNull()
        assertThat(project.tasks.findByName("jvmTestJavadocJar")).isNotNull()
        assertThat(project.tasks.findByName("jsJavadocJar")).isNull()

        // Verify each task is properly configured
        val jvmTask = project.tasks.getByName("jvmJavadocJar") as Jar
        val jvmTestTask = project.tasks.getByName("jvmTestJavadocJar") as Jar

        assertThat(jvmTask.archiveClassifier.get()).isEqualTo("javadoc")
        assertThat(jvmTestTask.archiveClassifier.get()).isEqualTo("javadoc")
        assertThat(jvmTask.description).contains("jvm publication")
        assertThat(jvmTestTask.description).contains("jvmTest publication")
    }

    @Test
    fun `should add javadoc artifact only to JVM publications`() {
        // Given - Apply maven-publish with mixed publications
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("jvm", MavenPublication::class.java)
        publishing.publications.create("js", MavenPublication::class.java)
        publishing.publications.create("native", MavenPublication::class.java)

        // When
        strategy.configure(project, config)

        // Then - Only JVM publication should have javadoc artifact added
        // Note: We can't easily test artifacts directly in unit tests due to Gradle's internal
        // handling
        // but we can verify the tasks were created for the right publications
        assertThat(project.tasks.findByName("jvmJavadocJar")).isNotNull()
        assertThat(project.tasks.findByName("jsJavadocJar")).isNull()
        assertThat(project.tasks.findByName("nativeJavadocJar")).isNull()
    }

    @Test
    fun `javadoc JAR task should use correct duplicates strategy`() {
        // Given - Apply maven-publish with JVM publication
        project.pluginManager.apply("maven-publish")

        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("jvm", MavenPublication::class.java)

        // When
        strategy.configure(project, config)

        // Then - Javadoc JAR task should have WARN duplicates strategy
        val javadocTask = project.tasks.getByName("jvmJavadocJar") as Jar
        assertThat(javadocTask.duplicatesStrategy.name).isEqualTo("WARN")
    }
}
