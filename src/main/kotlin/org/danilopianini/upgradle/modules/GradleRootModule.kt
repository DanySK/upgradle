package org.danilopianini.upgradle.modules

import org.danilopianini.upgradle.api.Module
import org.danilopianini.upgradle.api.Operation
import java.io.File

abstract class GradleRootModule: Module {

    protected val File.isGradleProject
        get() = listFiles()
            ?.any { it.isFile && (it.name == "build.gradle" || it.name == "build.gradle.kts") }
            ?: false

    final override fun operationsFor(localDirectory: File): List<Operation> =
        if (localDirectory.isGradleProject) {
            operationsInProjectRoot(localDirectory)
        } else {
            localDirectory.listFiles()
                ?.filter { it.isDirectory }
                ?.flatMap { operationsFor(it) }
                ?: emptyList()
        }

    abstract fun operationsInProjectRoot(projectRoot: File): List<Operation>

}
