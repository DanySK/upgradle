package org.danilopianini.upgradle.modules

import com.google.gson.Gson
import org.danilopianini.upgradle.CachedFor
import org.danilopianini.upgradle.UpGradle
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import kotlin.time.ExperimentalTime
import kotlin.time.hours

@ExperimentalTime
class GradleWrapper : GradleRootModule() {

    private val File.gradleWrapperProperties get() = File("$absolutePath/gradle/wrapper/gradle-wrapper.properties")

    override fun operationsInProjectRoot(projectRoot: File): List<Operation> {
        val properties = projectRoot.gradleWrapperProperties
        if (properties.exists() && properties.isFile) {
            val oldProperties = properties.readText()
            val versionMatch = Regex("gradle-($versionRegex).*.zip")
            val localGradleVersionMatch = versionMatch.find(oldProperties)?.groupValues
            if (localGradleVersionMatch != null && localGradleVersionMatch.size >= 2) {
                val localGradleVersion = localGradleVersionMatch[1]
                logger.info("Detected Gradle $localGradleVersion")
                if (localGradleVersion.startsWith(latestGradle)) {
                    logger.info("Version {} >= than the latest found {}", localGradleVersion, latestGradle)
                } else {
                    val description = "Upgrade Gradle Wrapper to $latestGradle"
                    val todo = SimpleOperation(
                        branch = "bump-gradle-wrapper-$localGradleVersion-to-$latestGradle",
                        commitMessage = description,
                        pullRequestTitle = description,
                        pullRequestMessage = "Gradle wrapper $localGradleVersion -> $latestGradle."
                    ) {
                        val newProperties = oldProperties.replace(versionMatch, "gradle-$latestGradle-bin.zip")
                        projectRoot.gradleWrapperProperties.writeText(newProperties)
                        listOf(OnFile(projectRoot.gradleWrapperProperties))
                    }
                    return listOf(todo)
                }
            } else {
                logger.warn("Unable to extract gradle version from ${properties.absolutePath}:\n$oldProperties")
            }
        } else {
            logger.warn("No Gradle wrapper descriptor available.")
        }
        return emptyList()
    }
    companion object {

        private val gson = Gson()
        private val logger = LoggerFactory.getLogger(UpGradle.javaClass)
        private const val versionRegex = """\d+(\.\d+)*"""

        val latestGradle: String by CachedFor(1.hours) {
            val response = URL("https://api.github.com/repos/gradle/gradle/releases").readText()
            val releases = gson.fromJson(response, List::class.java)
            releases.asSequence()
                .filterIsInstance<Map<String, String>>()
                .map { it["name"] }
                .filterNotNull()
                .first { it.matches(Regex(versionRegex)) }
        }
    }
}
