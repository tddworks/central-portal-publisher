package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPom
import org.gradle.internal.impldep.junit.framework.TestCase.assertEquals
import org.gradle.internal.impldep.junit.framework.TestCase.assertNotNull
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class ProjectExtensionsTest {

    @Test
    fun `should register correct JavadocJar task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("maven-publish")
        project.publishingExtension.publications.create("maven", MavenPublication::class.java)

        project.configureMavenPublication()

        val task = project.tasks.findByName("mavenJavadocJar")!! as Jar


        assertEquals("javadoc", task.archiveClassifier.get())
        assertEquals("maven", task.archiveAppendix.get())
        assertEquals("WARN", task.duplicatesStrategy.name)
    }

    @Test
    fun `should return correct maven pom configuration`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("maven-publish")
        project.publishingExtension.publications.create("maven", MavenPublication::class.java)

        project.configureMavenPublication()

        val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)

        assertNotNull(publishingExtension)

        val publications = publishingExtension.publications

        assertEquals(1, publications.size)

        publications.withType(MavenPublication::class.java).forEach {
            val pom = (it as MavenPublication).pom as DefaultMavenPom
            val scm = pom.scm

            assertEquals("Kotlin Multiplatform library template", pom.name.get())
            assertEquals("Dummy library to test deployment to Maven Central", pom.description.get())
            assertEquals("https://github.com/Kotlin/multiplatform-library-template", pom.url.get())
            assertEquals("https://github.com/Kotlin/multiplatform-library-template", scm.url.get())
            assertEquals("scm:git:git://github.com/Kotlin/multiplatform-library-template.git", scm.connection.get())
        }
    }
}