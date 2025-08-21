package com.tddworks.sonatype.publish.portal.plugin.provider

import com.tddworks.sonatype.publish.portal.plugin.configureIfExists
import com.tddworks.sonatype.publish.portal.plugin.configureMavenPublication
import com.tddworks.sonatype.publish.portal.plugin.get
import com.tddworks.sonatype.publish.portal.plugin.publishingExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import kotlin.jvm.java

internal inline val Project.gradlePublishing: PublishingExtension
    get() = extensions.getByType(PublishingExtension::class.java)

internal fun Project.mavenPublications(action: Action<MavenPublication>) {
    gradlePublishing.publications.withType(MavenPublication::class.java).configureEach(action)
}

interface PublicationProvider {
    fun preparePublication(project: Project)
}

class KotlinMultiplatformPublicationProvider : PublicationProvider {
    override fun preparePublication(project: Project) {
        // Kotlin Multiplatform
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            project.configureMavenPublication()
        }

        check(project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            "Calling configure(KotlinMultiplatform(...)) requires the org.jetbrains.kotlin.multiplatform plugin to be applied"
        }

        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            withSourcesJar(true)
        }

    }
}

class JvmPublicationProvider : PublicationProvider {
    override fun preparePublication(project: Project) {

        // central.sonatype.com - Sources must be provided but not found in entries
        project.plugins.withId("java") {
            project.configureIfExists<JavaPluginExtension> {
                withSourcesJar()
            }
        }


        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val javadocJar by project.tasks.registering(Jar::class) {
                archiveClassifier.set("javadoc")
                duplicatesStrategy = DuplicatesStrategy.WARN
                // contents are deliberately left empty
            }

            val publishing = project.publishingExtension

            publishing.publications.register<MavenPublication>("maven") {
                from(project.components["java"])
                configurePom(project)
            }

            // add javadocJar to the maven publication
            publishing.publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)
            }
        }
    }
}

fun MavenPublication.configurePom(project: Project) {
    // Provide artifacts information required by Maven Central
    pom {
        name = project.get("POM_NAME", "Kotlin Multiplatform library template")
        description = project.get(
            "POM_DESCRIPTION",
            "Dummy library to test deployment to Maven Central"
        )
        url = project.get(
            "POM_URL",
            "https://github.com/Kotlin/multiplatform-library-template"
        )

        licenses {
            license {
                name = project.get("POM_LICENCE_NAME", "MIT")
                url =
                    project.get("POM_LICENCE_URL", "https://opensource.org/licenses/MIT")
                distribution = project.get("POM_LICENCE_DIST", "repo")
            }
        }
        developers {
            developer {
                id = project.get("POM_DEVELOPER_ID", "JetBrains")
                name = project.get("POM_DEVELOPER_NAME", "JetBrains Team")
                email = project.get("POM_DEVELOPER_EMAIL", "")
                organization = project.get("POM_DEVELOPER_ORGANIZATION", "JetBrains")
                organizationUrl = project.get(
                    "POM_DEVELOPER_ORGANIZATION_URL",
                    "https://www.jetbrains.com"
                )
            }
        }
        scm {
            url = project.get(
                "POM_SCM_URL",
                "https://github.com/Kotlin/multiplatform-library-template"
            )
            connection =
                project.get(
                    "POM_SCM_CONNECTION",
                    "scm:git:git://github.com/Kotlin/multiplatform-library-template.git"
                )
            developerConnection = project.get(
                "POM_SCM_DEV_CONNECTION",
                "scm:git:git://github.com/Kotlin/multiplatform-library-template.git"
            )
        }

        issueManagement {
            system = project.get("POM_ISSUE_SYSTEM", "GitHub")
            url = project.get(
                "POM_ISSUE_URL",
                "https://github.com/Kotlin/multiplatform-library-template/issues"
            )
        }
    }
}
