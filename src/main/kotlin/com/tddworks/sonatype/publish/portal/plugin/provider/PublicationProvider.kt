package com.tddworks.sonatype.publish.portal.plugin.provider

import com.tddworks.sonatype.publish.portal.plugin.MavenPublicationConfigExtension
import com.tddworks.sonatype.publish.portal.plugin.configureIfExists
import com.tddworks.sonatype.publish.portal.plugin.publishingExtension
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.lang.UnsupportedOperationException

interface PublicationProvider {
    fun preparePublication(project: Project)
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
                configurePom()
            }

            // add javadocJar to the maven publication
            publishing.publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)
            }
        }
    }
}

fun Project.configureMavenPublication(
    groupId: String,
    artifactId: String,
    name: String
) {
    extensions.configure<PublishingExtension> {
        publications {
            all {
                val publication = this as MavenPublication

                //work around to fix an android publication artifact ID
                //https://youtrack.jetbrains.com/issue/KT-53520
                afterEvaluate {
                    publication.groupId = groupId
                    publication.mppArtifactId = artifactId
                }

                pom {
                    this.name.set(name)
                    url.set("https://github.com/JetBrains/compose-jb")
                    licenses {
                        license {
                            this.name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
}

var MavenPublication.mppArtifactId: String
    get() = throw UnsupportedOperationException()
    set(value) {
        val target = this.name
        artifactId = when (target) {
            "kotlinMultiplatform" -> value
            "androidRelease" -> "$value-android"
            else -> "$value-$target"
        }
    }

fun Project.configureMavenPublication(
    publicationName: String,
    config: MavenPublicationConfigExtension,
    customize: MavenPublication.() -> Unit = {},
) {
    // maven publication for plugin
    configureIfExists<PublishingExtension> {
        publications.create<MavenPublication>(publicationName) {
            artifactId = config.artifactId
            pom {
                name.set(config.displayName)
                description.set(config.description)
                url.set("https://www.jetbrains.com/lp/compose")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }

            customize()
        }
    }
}

fun MavenPublication.configurePom() {
    val githubRepo = "github.com/tddworks/openai-kotlin"
    pom {
        name = "test"
//        description = provider { project.description }
        description = "OpenAI API KMP Client"
        inceptionYear = "2023"
        url = "https://github.com/tddworks/openai-kotlin"

        developers {
            developer {
                name = "tddworks"
                email = "itshan@tddworks.com"
                organization = "tddworks team"
                organizationUrl = "www.tddworks.com"
            }
        }

        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        scm {
            url = githubRepo
            connection = "scm:git:$githubRepo.git"
            developerConnection = "scm:git:$githubRepo.git"
        }
    }
}