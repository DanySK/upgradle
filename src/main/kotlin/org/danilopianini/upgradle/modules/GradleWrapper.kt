package org.danilopianini.upgradle.modules

import com.google.gson.Gson
import org.danilopianini.upgradle.CachedFor
import org.danilopianini.upgradle.api.Module
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import java.io.File
import java.net.URL
import kotlin.time.ExperimentalTime
import kotlin.time.hours

@ExperimentalTime
class GradleWrapper() : GradleRootModule() {

    private val File.gradleWrapperProperties get() = File("$absolutePath/gradle/wrapper/gradle-wrapper.properties")

    override fun operationsInProjectRoot(projectRoot: File): List<Operation> {
        val properties = projectRoot.gradleWrapperProperties
        if (properties.exists() && properties.isFile) {
            val oldProperties = properties.readText()
            val versionMatch = Regex("gradle-($versionRegex).*.zip")
            val localGradleVersionMatch = versionMatch.find(oldProperties)?.groupValues
            if (localGradleVersionMatch != null && localGradleVersionMatch.size >= 2) {
                val localGradleVersion = localGradleVersionMatch[1]
                println("Detected Gradle $localGradleVersion")
                if (localGradleVersion.startsWith(latestGradle)) {
                    println("Version $localGradleVersion seems to be the same or newer than the latest found $latestGradle")
                } else {
                    val description = "Upgrade Gradle Wrapper to $latestGradle"
                    val todo = SimpleOperation(
                        branch = "bump-gradle-wrapper-$localGradleVersion-to-$latestGradle",
                        commitMessage = description,
                        pullRequestTitle = description,
                        pullRequestMessage = "Upgrades the gradle wrapper from $localGradleVersion to $latestGradle. Courtesy of UpGradle."
                    ) {
                        val newProperties = oldProperties.replace(versionMatch, "gradle-$latestGradle-bin.zip")
                        projectRoot.gradleWrapperProperties.writeText(newProperties)
                        listOf(OnFile(projectRoot.gradleWrapperProperties))
                    }
                    return listOf(todo)
                }
            } else {
                println("Unable to extract gradle version from ${properties.absolutePath}:\n$oldProperties")
            }
        } else {
            println("No Gradle wrapper descriptor available.")
        }
        return emptyList()
    }
    companion object {
        private val versionRegex = """\d+(\.\d+)*"""
        private val gson = Gson()
        val latestGradle: String by CachedFor(1.hours) {
            val response = URL("https://api.github.com/repos/gradle/gradle/releases").readText()
            val releases = gson.fromJson(response, List::class.java)
            releases.asSequence()
                .map { it as Map<String, String> }
                .map { it["name"] }
                .filterNotNull()
                .first { it.matches(Regex(versionRegex)) }
        }
    }
}