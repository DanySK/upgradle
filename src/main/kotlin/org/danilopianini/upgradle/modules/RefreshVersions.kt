package org.danilopianini.upgradle.modules

import arrow.core.extensions.sequence.foldable.isEmpty
import org.apache.commons.io.IOUtils
import org.danilopianini.upgradle.api.Module.ListExtensions.filterByStrategy
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class RefreshVersions(options: Map<String, Any>) : GradleRootModule() {

    private val strategy: String by options.withDefault { "next" }
    private val versionRegex: String by options.withDefault { ".*" }
    private val timeoutInMinutes: Long by options.withDefault { defaultWaitTime }
    private val validVersionRegex = versionRegex.toRegex()

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
                        logger.error(
                            """
                            |
                            |Could not refresh versions in ${projectRoot.path}, process exited with ${refresh.code}.
                            |
                            |${refresh.describeOutput}
                            |""".trimMargin()
                        )
                        emptyList()
                    }
                    is ProcessOutcome.TimeOut -> {
                        logger.error(
                            """
                            |
                            |RefreshVersions timed out on ${projectRoot.path}.
                            |
                            |${refresh.describeOutput}
                            |""".trimMargin()
                        )
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
        val dependenciesWithUpdates = extractUpdatesRegex.findAll(versionsContent)
        if (dependenciesWithUpdates.isEmpty()) {
            logger.info("No updates available")
        }
        return dependenciesWithUpdates.flatMap { match ->
            val (descriptor, artifact, old, candidates) = match.destructured
            extractVersionsRegex.findAll(candidates)
                .map { it.destructured.component1() }
                .filter { version -> validVersionRegex.matches(version) }
                .filterByStrategy(strategy)
                .map { new ->
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
                }
        }.toList()
    }

    private fun runRefresh(projectRoot: File): ProcessOutcome {
        val process = ProcessBuilder(listOf(gradleCommand, taskName))
            .directory(projectRoot)
            .start()
        return if (process.waitFor(timeoutInMinutes, TimeUnit.MINUTES)) {
            when (process.exitValue()) {
                0 -> ProcessOutcome.Ok
                else -> ProcessOutcome.Error(process)
            }
        } else {
            ProcessOutcome.TimeOut(process)
        }
    }

    companion object {
        private const val versionFileName = "versions.properties"
        private const val taskName = "refreshVersions"
        private const val defaultWaitTime = 5L
        private const val extractVersions =
            """\s*##\s*# available=(\S+)\R?"""
        private const val extractUpdates =
            """\s*((?:version|plugin)\.(?:.*\.)?([a-zA-Z].*))=(\S+)\R(\s*##\s*# available=(?:\S+)\R?)+"""
        internal val extractUpdatesRegex = Regex(extractUpdates)
        internal val extractVersionsRegex = Regex(extractVersions)
        private val logger = LoggerFactory.getLogger(RefreshVersions::class.java)
        private val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
        private val executable = "gradlew${ ".bat".takeIf { isWindows } ?: "" }"
        private val gradleCommand = "${"./".takeUnless { isWindows } ?: ""}$executable"
    }

    private sealed class ProcessOutcome(val output: String = "", val error: String = "") {

        val describeOutput get() =
            """
            |Process output:
            |$output
            |
            |Process error:
            |$error
            """.trimMargin()

        object Ok : ProcessOutcome()
        class Error(process: Process) : ProcessOutcome(process.inputStream.asString(), process.errorStream.asString()) {
            val code = process.exitValue()
        }
        class TimeOut(process: Process) : ProcessOutcome(process.inputStream.asString(), process.errorStream.asString())

        companion object {
            private fun InputStream.asString() = IOUtils.toString(this, StandardCharsets.UTF_8)
        }
    }
}
