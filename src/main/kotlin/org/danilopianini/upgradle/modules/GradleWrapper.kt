package org.danilopianini.upgradle.modules

import org.danilopianini.upgradle.CachedFor
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import org.danilopianini.upgradle.modules.GradleVersion.Companion.asGradleVersion
import java.io.File
import java.net.URL
import kotlin.time.ExperimentalTime
import kotlin.time.hours

@ExperimentalTime
class GradleWrapper(options: Map<String, Any>) : GradleRootModule(options) {

    override fun operationsInProjectRoot(projectRoot: File, projectId: String): List<Operation> {
        val properties = projectRoot.gradleWrapperProperties
        if (properties.exists() && properties.isFile) {
            val oldProperties = properties.readText()
            val versionMatch = Regex("gradle-${GradleVersion.distVersionRegex}.*.zip")
            val localGradleVersion = versionMatch.find(oldProperties)?.asGradleVersion
            if (localGradleVersion != null) {
                logger.info("Detected Gradle $localGradleVersion")
                val nextGradle = gradleVersions
                    .filter { it > localGradleVersion && validVersionRegex.matches(it.toString()) }
                    .filterByStrategy()
                if (nextGradle.iterator().hasNext()) {
                    logger.info("Gradle can be updated to: {}", nextGradle)
                } else {
                    logger.info("The Gradle wrapper looks up to date here ($localGradleVersion)")
                }
                return nextGradle.map { newerGradle ->
                    val description = "Upgrade Gradle Wrapper to $newerGradle${inProject(projectId)}"
                    val pullRequestMessage = """
                        This pull request upgrades the Gradle wrapper in project ${ inProject(projectId) }
                        from $localGradleVersion to $newerGradle.                       
                    """.trimIndent()
                    SimpleOperation(
                        branch = "bump-gradle-wrapper-to-$newerGradle${projectDescriptor(projectId)}",
                        commitMessage = description,
                        pullRequestTitle = description,
                        pullRequestMessage = pullRequestMessage
                    ) {
                        val newProperties = oldProperties.replace(versionMatch, "gradle-$newerGradle-bin.zip")
                        projectRoot.gradleWrapperProperties.writeText(newProperties)
                        listOf(OnFile(projectRoot.gradleWrapperProperties))
                    }
                }.toList()
            } else {
                logger.warn("Unable to extract gradle version from ${properties.absolutePath}:\n$oldProperties")
            }
        } else {
            logger.warn("No Gradle wrapper descriptor available.")
        }
        return emptyList()
    }
    companion object {

        private val versionExtractor = Regex("""gradle-${GradleVersion.distVersionRegex}-bin\.zip""")
        private val File.gradleWrapperProperties get() = File("$absolutePath/gradle/wrapper/gradle-wrapper.properties")

        val gradleVersions: Sequence<GradleVersion> by CachedFor(1.hours) {
            val response = URL("https://services.gradle.org/distributions/").readText()
            versionExtractor.findAll(response)
                .map {
                    val (major, minor, _, patch, _, rc) = it.destructured
                    GradleVersion(major, minor, patch, rc)
                }
                .distinct()
                .sortedDescending()
        }
    }
}

data class GradleVersion(
    val major: Int,
    val minor: Int,
    val patch: Int? = null,
    val rc: Int? = null
) : Comparable<GradleVersion> {

    constructor (
        major: String,
        minor: String,
        patch: String? = null,
        rc: String? = null
    ) : this (major.toInt(), minor.toInt(), patch?.toIntOrNull(), rc?.toIntOrNull())

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

    override fun toString() = "$major.$minor${ patch?.let { ".$it" } ?: "" }${ rc?.let { "-rc-$it" } ?: "" }"

    companion object {

        val distVersionRegex = Regex("""(\d+)\.(\d+)(\.(\d+))?(-rc-(\d*))?""")
        private val ghRegex = Regex("""(\d+)\.(\d+)(\.(\d+))?( RC(\d+))?""")

        fun fromGithubRelease(tagName: String): GradleVersion? = ghRegex.extract(tagName)

        fun fromGradleDistribution(version: String): GradleVersion? = distVersionRegex.extract(version)

        val MatchResult.asGradleVersion: GradleVersion? get() = destructured.let { (major, minor, _, patch, _, rc) ->
            GradleVersion(major, minor, patch, rc)
        }

        private fun Regex.extract(descriptor: String) = find(descriptor)?.asGradleVersion

        private fun Int?.compareWith(other: Int?): Int = when {
            this == null && other == null -> 0
            this == null -> -1
            other == null -> 1
            else -> compareTo(other)
        }

        private fun Int?.compareOrNull(other: Int?): Int? = compareWith(other).takeIf { it != 0 }
    }
}
