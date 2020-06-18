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
            else -> map { file ->
                val projectId = file.relativeTo(localDirectory).toString()
                    // They cannot have two consecutive dots .. anywhere.
                    .replace("..", "_doubledot_")
                    // No ASCII control chars (i.e. bytes values are lower than \040, or \177 DEL)
                    .filter { it.toInt() in ' '.toInt()..'~'.toInt() }
                    // no slash-separated component can begin with a dot . or end with the sequence .lock
                    .replace(".lock", "_dotlock_")
                    // Remap forbidden chars
                    .map { forbiddenCharsSubstitutions.getOrDefault(it, it) }
                    .joinToString(separator = "")
                operationsInProjectRoot(file, projectId)
            }.flatten()
        }
    }

    abstract fun operationsInProjectRoot(projectRoot: File, projectId: String = defaultProjectId): List<Operation>

    companion object {
        const val defaultProjectId = "root"
        private val forbiddenCharsSubstitutions = mapOf(
            ' ' to "_",
            '\\' to "_backslash_",
            '~' to "_tilde_",
            '?' to "_questionmark_",
            '@' to "_at_",
            '*' to "_star_",
            '[' to "_openbracket_",
            '^' to "_caret_",
            ':' to "_colon_",
            '.' to "_dot_"
        )
        fun inProject(projectId: String) = if (projectId == defaultProjectId) "" else " in $projectId"
        fun projectDescriptor(projectId: String) = inProject(projectId).replace(" ", "-")
    }
}
