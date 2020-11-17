package org.danilopianini.upgradle.modules

import arrow.core.extensions.sequence.foldable.isEmpty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.apache.commons.io.IOUtils
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.TimeUnit

class RefreshVersions(options: Map<String, Any>) : GradleRootModule(options) {

    private val timeoutInMinutes: Long by options.withDefault { defaultWaitTime }

    @ExperimentalCoroutinesApi
    override fun operationsInProjectRoot(projectRoot: File, projectId: String): List<Operation> {
        val filesInRoot = projectRoot.listFiles()?.filter { it.isFile } ?: emptyList()
        val versionsFile = filesInRoot.find { it.name == versionFileName }
        // Check for gradlew and gradlew.bat
        if (versionsFile?.exists() == true) {
            val execFile = filesInRoot.find { it.name == executable }
            if (execFile?.exists() == true) {
                // If under *nix, make gradlew executable
                try {
                    val originalPermissions: Set<PosixFilePermission> = Files.getPosixFilePermissions(execFile.toPath())
                    if (!originalPermissions.containsAll(executeRights)) {
                        Files.setPosixFilePermissions(execFile.toPath(), originalPermissions + executeRights)
                    }
                } catch (unsupported: UnsupportedOperationException) {
                    logger.warn("The local file system does not support Unix permissions")
                }
                val originalVersions = versionsFile.readText()
                logger.info("Running refreshVersions")
                when (val refresh = runRefresh(projectRoot)) {
                    is ProcessOutcome.Error -> {
                        logger.error(
                            """
                            |
                            |Could not refresh versions in ${projectRoot.path}, process exited with ${refresh.code}.
                            |UpGradle will attempt to generate updates anyway, but you should get this fixed.
                            |
                            |${refresh.describeOutput}
                            |""".trimMargin()
                        )
                    }
                    is ProcessOutcome.TimeOut -> {
                        logger.error(
                            """
                            |
                            |RefreshVersions timed out on ${projectRoot.path}.
                            |UpGradle will attempt to generate updates anyway
                            |(maybe the time was sufficient for refreshVersion to run).
                            |
                            |${refresh.describeOutput}
                            |""".trimMargin()
                        )
                    }
                    is ProcessOutcome.Ok -> logger.info("Version refresh successful. Extracting available updates")
                }
                return prepareUpdates(projectId, versionsFile, originalVersions)
            } else {
                logger.warn("No {} file available in {}", execFile, projectRoot.absolutePath)
            }
        } else {
            logger.warn("No {} file available in {}", versionFileName, projectRoot.absolutePath)
        }
        return emptyList()
    }

    internal fun prepareUpdates(projectId: String, versionsFile: File, originalVersions: String): List<Operation> {
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
                .filterByStrategy()
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

    @ExperimentalCoroutinesApi
    private fun runRefresh(projectRoot: File): ProcessOutcome {
        val process = ProcessBuilder(listOf(gradleCommand, "--console=plain", taskName))
            .directory(projectRoot)
            .start()
        val output = GlobalScope.async(Dispatchers.IO) {
            IOUtils.toString(process.inputStream, StandardCharsets.UTF_8)
        }
        val error = GlobalScope.async(Dispatchers.IO) {
            IOUtils.toString(process.errorStream, StandardCharsets.UTF_8)
        }
        return if (process.waitFor(timeoutInMinutes, TimeUnit.MINUTES)) {
            when (process.exitValue()) {
                0 -> ProcessOutcome.Ok
                else -> ProcessOutcome.Error(process, output.getCompleted(), error.getCompleted())
            }
        } else {
            logger.error("One process has timed out")
            process.destroy()
            if (!process.waitFor(1L, TimeUnit.SECONDS)) {
                logger.error("One process refused to terminate after timeout, destroying forcibly")
                process.destroyForcibly()
                ProcessOutcome.TimeOut()
            } else {
                ProcessOutcome.TimeOut(output.getCompleted(), error.getCompleted())
            }
        }
    }

    companion object {
        private const val versionFileName = "versions.properties"
        private const val taskName = "refreshVersions"
        private const val defaultWaitTime = 5L
        private const val extractVersions =
            """\s*##\s*# available=(\S+)\R?"""
        private const val extractUpdates =
            """\s*((?:version|plugin)\.(?:.*\.)?([a-zA-Z].*))=(\S+)\R((?:\s*##\s*# available=(?:\S+)\R?)+)"""
        internal val extractUpdatesRegex = Regex(extractUpdates)
        internal val extractVersionsRegex = Regex(extractVersions)
        private val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
        private val executable = "gradlew${ ".bat".takeIf { isWindows } ?: "" }"
        private val gradleCommand = "${"./".takeUnless { isWindows } ?: ""}$executable"
        private val executeRights: Set<PosixFilePermission> = PosixFilePermission.values()
            .filter { it.name.endsWith("execute", ignoreCase = true) }
            .toSet()
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

        class Error(process: Process, output: String, error: String) : ProcessOutcome(output, error) {
            val code = process.exitValue()
        }

        class TimeOut(
            output: String = "Unable to fetch output: forcibly terminated",
            error: String = output
        ) : ProcessOutcome(output, error)
    }
}
