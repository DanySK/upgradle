package org.danilopianini.upgradle.modules

import arrow.core.extensions.sequence.foldable.isEmpty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ResultHandler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.concurrent.CompletableFuture

private typealias GradleRunResult = Triple<Boolean, OutputStream, OutputStream>
val GradleRunResult.isSuccess: Boolean get() = first
val GradleRunResult.output: OutputStream get() = second
val GradleRunResult.error: OutputStream get() = third
fun GradleRunResult.onFailure(operation: (GradleRunResult) -> Unit): GradleRunResult =
    this.apply { if (!isSuccess) operation(this) }
fun GradleRunResult.onSuccess(operation: (GradleRunResult) -> Unit): GradleRunResult =
    this.apply { if (isSuccess) operation(this) }

class RefreshVersions(options: Map<String, Any>) : GradleRootModule(options) {

    @ExperimentalCoroutinesApi
    override fun operationsInProjectRoot(projectRoot: File, projectId: String): List<Operation> {
        val filesInRoot = projectRoot.listFiles()?.filter { it.isFile } ?: emptyList()
        val versionsFile = filesInRoot.find { it.name == versionFileName }
        // Check for gradlew and gradlew.bat
        if (versionsFile?.exists() == true) {
            val originalVersions = versionsFile.readText()
            val execFile = filesInRoot.find { it.name == executable }
            if (execFile?.exists() == true) {
                logger.info("Running refreshVersions")
                runRefresh(projectRoot)
                    .onFailure {
                        logger.error("Could not refresh versions in {}.")
                        logger.error("Output Stream:\n{}", it.output)
                        logger.error("Error Stream:\n{}", it.error)
                    }
                    .onSuccess { logger.info("Version refresh successful. Extracting available updates") }
                return prepareUpdates(projectId, versionsFile, originalVersions)
            } else {
                logger.warn("No {} file available in {}", execFile, projectRoot.absolutePath)
            }
        } else {
            logger.warn("No {} file available in {}", versionFileName, projectRoot.absolutePath)
        }
        return emptyList()
    }

    private fun prepareUpdates(projectId: String, versionsFile: File, originalVersions: String): List<Operation> {
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
    private fun runRefresh(projectRoot: File): GradleRunResult {
        val connection = GradleConnector.newConnector().forProjectDirectory(projectRoot).connect()
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        val finalResult: CompletableFuture<GradleRunResult> = CompletableFuture()
        connection.newBuild()
            .forTasks(taskName)
            .setStandardError(errorStream)
            .setStandardOutput(outputStream)
            .run(object : ResultHandler<Any> {
                override fun onComplete(result: Any?) {
                    finalResult.complete(GradleRunResult(true, outputStream, errorStream))
                }
                override fun onFailure(failure: GradleConnectionException) {
                    finalResult.complete(GradleRunResult(false, outputStream, errorStream))
                }
            })
        return finalResult.get()
    }

    companion object {
        private const val versionFileName = "versions.properties"
        private const val taskName = "refreshVersions"
        private const val extractVersions =
            """\s*##\s*# available=(\S+)\R?"""
        private const val extractUpdates =
            """\s*((?:version|plugin)\.(?:.*\.)?([a-zA-Z].*))=(\S+)\R((?:\s*##\s*# available=(?:\S+)\R?)+)"""
        internal val extractUpdatesRegex = Regex(extractUpdates)
        internal val extractVersionsRegex = Regex(extractVersions)
        private val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
        private val executable = "gradlew${ ".bat".takeIf { isWindows } ?: "" }"
    }
}
