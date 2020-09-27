package org.danilopianini.upgradle.modules

import org.danilopianini.upgradle.CachedFor
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.net.URL
import kotlin.time.ExperimentalTime
import kotlin.time.hours

class TravisDist(options: Map<String, Any> = emptyMap()) : AbstractModule(options) {
    @ExperimentalTime
    override fun invoke(localDirectory: File): List<Operation> =
        localDirectory.listFiles()
            ?.find { it.name == ".travis.yml" }
            ?.run {
                val travisYmlContent = readText()
                val configuration = runCatching {
                    Yaml().load<Map<String, Any>>(travisYmlContent)
                }.getOrElse {
                    logger.warn("Invalid YAML file: ${this.path}")
                    travisYmlContent.lineSequence().forEach(logger::warn)
                    logger.warn("Exception would have been the following", it)
                    emptyMap()
                }
                (configuration[dist] as? String) ?.let { currentDist ->
                    availableDistributions.takeLastWhile { it != currentDist }
                        .asSequence()
                        .filterByStrategy()
                        .map { newDist ->
                            SimpleOperation(
                                branch = "bump-travis-dist-to-$newDist",
                                commitMessage = "Update travis 'dist' to $newDist",
                                pullRequestTitle = "Use 'dist: $newDist' on Travis",
                                pullRequestMessage = "Update the Ubuntu version on Travis CI to ${newDist.capitalize()}"
                            ) {
                                currentDist.toRegex().findAll(travisYmlContent)
                                    .map { travisYmlContent.replaceRange(it.range, newDist) }
                                    .filter {
                                        runCatching {
                                            val newConfig = Yaml().load<Map<String, Any>>(it)
                                            newConfig[dist] == newDist
                                        }.getOrElse { false }
                                    }
                                    .firstOrNull()
                                    ?.let {
                                        writeText(it)
                                        listOf(OnFile(this))
                                    }
                                    ?: emptyList()
                            }
                        }
                        .toList()
                }
            }
            ?: emptyList()

    companion object {

        private const val wikipediaUbuntuVersionHistoryURL =
            "https://en.wikipedia.org/wiki/Ubuntu_version_history"
        private const val travisDistRbURL =
            "https://raw.githubusercontent.com/travis-ci/travis-yml/master/lib/travis/yml/schema/def/dist.rb"
        private const val dist = "dist"
        private val extractVersionFromTravis =
            """value\s*:\s*(\w+)\s*,\s*only\s*:\s*\{\s*os\s*:.*:linux""".toRegex()
        private val extractVersionFromWikipedia =
            """href="#Ubuntu_\d+\.\d+_LTS_\((\w+)_\w+\)""".toRegex()
        @ExperimentalTime
        val availableDistributions: List<String> by CachedFor(1.hours) {
            val rubyDescriptor = URL(travisDistRbURL).readText()
            val availableOnTravis = extractVersionFromTravis.findAll(rubyDescriptor).asSequence()
                .map { it.destructured.component1() }
                .toSet()
            val wikipediaPage = URL(wikipediaUbuntuVersionHistoryURL).readText()
            extractVersionFromWikipedia.findAll(wikipediaPage).asSequence()
                .map { it.destructured.component1().toLowerCase() }
                .filter { it in availableOnTravis }
                .toList()
        }
    }
}
