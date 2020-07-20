package org.danilopianini.upgradle.modules

import arrow.core.extensions.sequence.foldable.isEmpty
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import org.slf4j.LoggerFactory
import java.io.File

class RefreshVersions : GradleRootModule() {

    override fun operationsInProjectRoot(projectRoot: File, projectId: String): List<Operation> {
        val filesInRoot = projectRoot.listFiles()?.filter { it.isFile } ?: emptyList()
        val versionsFile = filesInRoot.find { it.name == versionFileName }
        // Check for gradlew and gradlew.bat
        if (versionsFile?.exists() == true) {
            val execFile = filesInRoot.find { it.name == executable }
            if (execFile?.exists() == true) {
                val originalVersions = versionsFile.readText()
                logger.info("Running refreshVersions")
                return when (val refresh = runRefresh(projectRoot)) {
                    is ProcessOutcome.Error -> {
                        logger.error("Could not refresh versions, process ended with error ${refresh.code}.")
                        emptyList()
                    }
                    is ProcessOutcome.Ok -> prepareUpdates(projectId, versionsFile, originalVersions)
                }
            } else {
                logger.warn("No {} file available in {}", execFile, projectRoot.absolutePath)
            }
        } else {
            logger.warn("No {} file available in {}", versionFileName, projectRoot.absolutePath)
        }
        return emptyList()
    }

    private fun prepareUpdates(projectId: String, versionsFile: File, originalVersions: String): List<Operation> {
        logger.info("Version refresh successful. Extracting available updates")
        val versionsContent = versionsFile.readText()
        val matches = updateExtractionRegex.findAll(versionsContent)
        if (matches.isEmpty()) {
            logger.info("No updates available")
        }
        return matches.map { match ->
            val (descriptor, artifact, old, new) = match.destructured
            logger.info("Found update for {}: {} -> {}", artifact, old, new)
            val message = "Upgrade $artifact from $old to $new${inProject(projectId)}"
            SimpleOperation(
                branch = "bump-$artifact-to-$new${projectDescriptor(projectId)}",
                commitMessage = message,
                pullRequestTitle = message,
                pullRequestMessage = "This update was prepared for you by UpGradle, at your service."
            ) {
                val newVersionRegex = new.map { if (it in """\^$,.|?*+()[]{}""") """\$it""" else "$it" }
                    .joinToString(separator = "")
                val updateLineRegex = Regex("""##\s*# available=$newVersionRegex\n""")
                val updated = originalVersions
                    .replace("$descriptor=$old", "$descriptor=$new")
                    .replace(updateLineRegex, "")
                logger.info("Updating the version file")
                versionsFile.writeText(updated)
                logger.info("Version file updated")
                listOf(OnFile(versionsFile))
            }
        }.toList()
    }

    companion object {
        private const val versionFileName = "versions.properties"
        private const val taskName = "refreshVersions"
        private const val pluginPrefix = "plugin."
        private const val versionPrefix = "version."
        private const val extractor =
            """\s*((?:version|plugin)\.(?:.*\.)?([a-zA-Z].*))=(\S*)\s*##\s*# available=(\S*)"""
        val updateExtractionRegex = Regex(extractor)
        private val logger = LoggerFactory.getLogger(RefreshVersions::class.java)
        private val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
        private val executable = "gradlew${ ".bat".takeIf { isWindows } ?: "" }"
        private val gradleCommand = "${"./".takeUnless { isWindows } ?: ""}$executable"

        private fun runRefresh(projectRoot: File): ProcessOutcome = ProcessBuilder(listOf(gradleCommand, taskName))
            .directory(projectRoot)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
            .let { if (it == 0) ProcessOutcome.Ok else ProcessOutcome.Error(it) }
    }

    private sealed class ProcessOutcome {
        object Ok : ProcessOutcome()
        class Error(val code: Int) : ProcessOutcome()
    }
}
