package com.tddworks.sonatype.portal.plugin.task

import com.tddworks.sonatype.publish.portal.api.PublicationType
import com.tddworks.sonatype.publish.portal.plugin.tasks.PublishTask
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PublishTaskTest {

    @Test
    fun `should throw exception when username is empty`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("publishTask", PublishTask::class.java)

        task.username.set("")
        task.password.set("testPassword")
        task.publicationType.set(PublicationType.AUTOMATIC)

        task.inputFile.set(project.file("testFile.txt"))

        val exception = assertThrows(IllegalStateException::class.java) { task.taskAction() }

        assertEquals("SonatypePortal: username must not be empty", exception.message)
    }

    @Test
    fun `should throw exception when password is empty`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("publishTask", PublishTask::class.java)

        task.username.set("testUsername")
        task.password.set("")
        task.publicationType.set(PublicationType.AUTOMATIC)

        task.inputFile.set(project.file("testFile.txt"))

        val exception = assertThrows(IllegalStateException::class.java) { task.taskAction() }

        assertEquals("SonatypePortal: password must not be empty", exception.message)
    }
}
