package org.danilopianini.upgradle.modules

import org.danilopianini.upgradle.api.Module
import org.danilopianini.upgradle.api.Operation
import java.io.File

abstract class GradleRootModule : Module {

    protected val File.isGradleProject
        get() = listFiles()
            ?.any { it.isFile && (it.name == "build.gradle" || it.name == "build.gradle.kts") }
            ?: false

    protected fun File.gradleRoots(): List<File> = listOf(this).takeIf { isGradleProject }
            ?: listFiles()?.filter { it.isDirectory }?.flatMap { it.gradleRoots() }
            ?: emptyList()

    final override fun invoke(localDirectory: File): List<Operation> = with(localDirectory.gradleRoots()) {
        when {
            size <= 1 -> flatMap { operationsInProjectRoot(it) }
            else -> mapIndexed { index, file -> operationsInProjectRoot(file, "project-$index") }.flatten()
        }
    }

    abstract fun operationsInProjectRoot(projectRoot: File, projectId: String = defaultProjectId): List<Operation>

    companion object {
        const val defaultProjectId = "root"
        fun inProject(projectId: String) = if (projectId == defaultProjectId) "" else " in $projectId"
        fun projectDescriptor(projectId: String) = inProject(projectId).replace(" ", "-")
    }
}
