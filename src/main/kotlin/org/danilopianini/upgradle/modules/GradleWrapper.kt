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
                val nextGradle = gradleWrapperVersions.takeLastWhile { !localGradleVersion.startsWith(it) }
                if (nextGradle.isEmpty()) {
                    logger.info("The Gradle wrapper looks up to date here ($localGradleVersion)")
                } else {
                    logger.info("Gradle can be updated to: ${nextGradle}")
                }
                return nextGradle.map { newerGradle ->
                    val description = "Upgrade Gradle Wrapper to $newerGradle"
                    SimpleOperation(
                            branch = "bump-gradle-wrapper-$localGradleVersion-to-$newerGradle",
                            commitMessage = description,
                            pullRequestTitle = description,
                            pullRequestMessage = "Gradle wrapper $localGradleVersion -> $newerGradle."
                    ) {
                        val newProperties = oldProperties.replace(versionMatch, "gradle-$newerGradle-bin.zip")
                        projectRoot.gradleWrapperProperties.writeText(newProperties)
                        listOf(OnFile(projectRoot.gradleWrapperProperties))
                    }
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
        private val logger = LoggerFactory.getLogger(UpGradle::class.java)
        private const val versionRegex = """\d+(\.\d+)*(-rc\d*)?"""

        val gradleVersions: List<GradleVersion> by CachedFor(1.hours) {
            val response = URL("https://api.github.com/repos/gradle/gradle/releases").readText()
            val releases = gson.fromJson(response, List::class.java)
            releases.asSequence()
                .filterIsInstance<Map<String, String>>()
                .map { it["name"] }
                .filterNotNull()
                .map(GradleVersion.Companion::fromGithubRelease)
                .filterNotNull()
                .sorted()
                .toList()
        }

        val gradleWrapperVersions get() = gradleVersions.map { it.downloadReference }
    }
}

data class GradleVersion(val major: Int, val minor: Int, val patch: Int?, val rc: Int?) : Comparable<GradleVersion> {

    val downloadReference: String = "$major.$minor${
            patch?.let { ".$it" } ?: ""
        }${ rc?.let { "-rc-$it" } ?: "" }"

    companion object {
        private val regex = Regex("""(\d+)\.(\d+)(\.(\d+))?( RC(\d+))?""")

        fun fromGithubRelease(tagName: String): GradleVersion? = regex.find(tagName)?.let {
                val (major, minor, _, patch, _, rc) = it.destructured
                GradleVersion(major.toInt(), minor.toInt(), patch.toIntOrNull(), rc.toIntOrNull())
            }
    }

    override fun compareTo(other: GradleVersion) =
        major.compareOrNull(other.major)
            ?: minor.compareOrNull(other.minor)
            ?: patch.compareOrNull(other.patch)
            ?: when {
                rc == other.rc -> 0
                rc == null -> 1
                other.rc == null -> -1
                else -> rc.compareTo(other.rc)
            }

    private fun Int?.compareWith(other: Int?): Int = when {
        this == null && other == null -> 0
        this == null -> -1
        other == null -> 1
        else -> compareTo(other)
    }

    private fun Int?.compareOrNull(other: Int?): Int? = compareWith(other).takeIf { it != 0 }

}
