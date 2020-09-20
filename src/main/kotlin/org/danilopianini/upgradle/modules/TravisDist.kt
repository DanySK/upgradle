package org.danilopianini.upgradle.modules

import org.danilopianini.upgradle.CachedFor
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.api.SimpleOperation
import org.yaml.snakeyaml.DumperOptions
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
                val configuration = Yaml().load<Map<String, Any>>(readText())
                configuration["dist"]
                    ?.toString()
                    ?.let {currentDist ->
                        availableDistributions.takeLastWhile { it != currentDist }
                            .asSequence()
                            .filterByStrategy()
                            .map {
                                SimpleOperation(
                                    branch = "bump-travis-dist-to-$it",
                                    commitMessage = "Update travis 'dist' to $it",
                                    pullRequestTitle = "Use 'dist: $it' on Travis",
                                    pullRequestMessage = "Update the Ubuntu version on Travis CI to ${it.capitalize()}"
                                ) {
                                    val newConfiguration = configuration + ("dist" to it)
                                    val newConf = Yaml().dumpAs(newConfiguration, null, DumperOptions.FlowStyle.BLOCK)
                                    println(readText())
                                    println("----------------")
                                    println(newConf)
                                    println("##################")
                                    println(newConfiguration)
                                    listOf(OnFile(this))
                                }
                            }
                            .toList()
                }
                ?: emptyList()
            }
            ?: emptyList()

    companion object {

        private const val wikipediaUbuntuVersionHistoryURL =
            "https://en.wikipedia.org/wiki/Ubuntu_version_history"
        private const val travisDistRbURL =
            "https://raw.githubusercontent.com/travis-ci/travis-yml/master/lib/travis/yml/schema/def/dist.rb"
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
