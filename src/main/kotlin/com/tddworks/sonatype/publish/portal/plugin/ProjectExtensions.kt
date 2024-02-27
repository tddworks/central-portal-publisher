package com.tddworks.sonatype.publish.portal.plugin

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.*


internal const val EXTENSION_NAME = "sonatypePortalPublisher"

internal val Project.layoutBuildDir get() = layout.buildDirectory.get().asFile
internal val Project.layoutBuildDirectory get() = layout.buildDirectory
internal val Project.publishingExtension get() = extensions.getByType<PublishingExtension>()
internal val Project.sonatypePortalPublisherExtension get() = extensions.getByType<SonatypePortalPublisherExtension>()

internal val Project.createZipConfigurationConsumer
    get() = configurations.maybeCreate(ZIP_CONFIGURATION_CONSUMER).apply {
        isCanBeResolved = true
        isCanBeConsumed = false
        configureAttributes(project)
    }

internal val Project.createZipConfigurationProducer
    get() = configurations.maybeCreate(ZIP_CONFIGURATION_PRODUCER).apply {
        isCanBeConsumed = true
        isCanBeResolved = false
        configureAttributes(project)
    }


fun Project.configureMavenPublication() {
    extensions.configure<PublishingExtension> {
        // Configure all publications
        publications.withType<MavenPublication> {
            // Stub javadoc.jar artifact
            artifact(tasks.register("${name}JavadocJar", Jar::class) {
                archiveClassifier = "javadoc"
                archiveAppendix = this@withType.name
                duplicatesStrategy = DuplicatesStrategy.WARN
            })

            // Provide artifacts information required by Maven Central
            pom {
                name = get("POM_NAME", "Kotlin Multiplatform library template")
                description = get("POM_DESCRIPTION", "Dummy library to test deployment to Maven Central")
                url = get("POM_URL", "https://github.com/Kotlin/multiplatform-library-template")

                licenses {
                    license {
                        name = get("POM_LICENCE_NAME", "MIT")
                        url = get("POM_LICENCE_URL", "https://opensource.org/licenses/MIT")
                        distribution = get("POM_LICENCE_DIST", "repo")
                    }
                }
                developers {
                    developer {
                        id = get("POM_DEVELOPER_ID", "JetBrains")
                        name = get("POM_DEVELOPER_NAME", "JetBrains Team")
                        email = get("POM_DEVELOPER_EMAIL", "")
                        organization = get("POM_DEVELOPER_ORGANIZATION", "JetBrains")
                        organizationUrl = get("POM_DEVELOPER_ORGANIZATION_URL", "https://www.jetbrains.com")
                    }
                }
                scm {
                    url = get("POM_SCM_URL", "https://github.com/Kotlin/multiplatform-library-template")
                    connection =
                        get("POM_SCM_CONNECTION", "scm:git:git://github.com/Kotlin/multiplatform-library-template.git")
                    developerConnection = get(
                        "POM_SCM_DEV_CONNECTION",
                        "scm:git:git://github.com/Kotlin/multiplatform-library-template.git"
                    )
                }

                issueManagement {
                    system = get("POM_ISSUE_SYSTEM", "GitHub")
                    url = get("POM_ISSUE_URL", "https://github.com/Kotlin/multiplatform-library-template/issues")
                }
            }
        }
    }
}

fun Project.get(name: String, default: String = "$name not found") =
    properties[name]?.toString() ?: System.getenv(name) ?: default
