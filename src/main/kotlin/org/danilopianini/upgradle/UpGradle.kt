package org.danilopianini.upgradle

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import com.uchuhimo.konf.source.yaml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.danilopianini.upgradle.api.Credentials
import org.danilopianini.upgradle.api.Module
import org.danilopianini.upgradle.api.Module.Companion.asModule
import org.danilopianini.upgradle.api.Operation
import org.danilopianini.upgradle.config.Configurator
import org.danilopianini.upgradle.remote.Branch
import org.danilopianini.upgradle.remote.GraphqlSource
import org.danilopianini.upgradle.remote.Repository
import org.danilopianini.upgradle.remote.Selector
import org.danilopianini.upgradle.remote.graphql.FuelGithubClient
import org.eclipse.egit.github.core.client.RequestException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class UpGradle(configuration: Config.() -> Config = { from.yaml.resource("upgradle.yml") }) {
    val configuration = Configurator.load(configuration)

    @ExperimentalPathApi
    suspend fun runModule(repository: Repository, branch: Branch, module: Module, credentials: Credentials) {
        val user = repository.owner
        logger.info("Running ${module.name} on $user/${repository.name} on branch ${branch.name}")
        val workdirPrefix = "upgradle-${user}_${repository.name}_${branch.name}_${module.name}"
        val destination = createTempDirectory(workdirPrefix).toFile()
        logger.debug("Working inside ${destination.absolutePath}")
        val git = repository.clone(branch, destination, credentials)
        val branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map { it.name }
        logger.debug("Available branches: $branches")
        // Run the module
        module(destination)
            .asReversed()
            .asSequence()
            .filter { proposedUpdate -> branches.none { it.endsWith(proposedUpdate.branch) } }
            .forEach { update ->
                prepareRepository(git, branch, update)
                // Run the update operation
                val changes = update()
                logger.debug("Changes: {}", changes)
                git.add(destination, changes)
                // Commit changes
                git.commit(update.commitMessage, configuration.author)
                // Push the new branch
                logger.info("Pushing ${update.branch}...")
                val pushResults = git.pushTo(update.branch, credentials)
                // If push ok, create a pull request
                if (pushResults.isNotEmpty() && pushResults.all { it.status == RemoteRefUpdate.Status.OK }) {
                    logger.info("Push successful, creating a pull request")
                    try {
                        retry {
                            val head = update.branch
                            val base = branch.name
                            val pullRequest = repository.createPullRequest(update, head, base, credentials)
                            logger.info("Pull request #${pullRequest.number} opened ${update.branch} -> ${branch.name}")
                            repository.applyLabels(configuration.labels, pullRequest, credentials)
                        }
                    } catch (requestException: RequestException) {
                        when (requestException.status) {
                            UNPROCESSABLE_ENTITY -> println(requestException.message)
                            else -> throw requestException
                        }
                    }
                } else {
                    logger.error("Push failed.")
                    pushResults.map(Any::toString).forEach(logger::error)
                }
            }
        destination.deleteRecursively()
    }

    companion object {

        private const val UNPROCESSABLE_ENTITY = 422
        @OptIn(ExperimentalTime::class)
        private val DEFAULT_WAIT: Duration = Duration.Companion.minutes(5)
        internal val logger = LoggerFactory.getLogger(UpGradle::class.java)

        private fun upgradleFromArguments(args: Array<String>) = when (args.size) {
            0 -> UpGradle()
            1 -> File(args[0]).let { file ->
                val path = file.absolutePath
                if (!file.exists()) {
                    println("File $path does not exist")
                    exitProcess(2)
                }
                if (file.isDirectory) {
                    println("File $path is a directory")
                    exitProcess(3)
                }
                UpGradle {
                    when (file.extension) {
                        in setOf("yml", "yaml") -> from.yaml
                        "json" -> from.json
                        "toml" -> from.toml
                        else -> {
                            println("Unsupported file type: ${file.extension}")
                            exitProcess(4)
                        }
                    }.file(file)
                }
            }
            else -> {
                println("A single parameter is required with the path to the configuration file.")
                println("If no parameter is provided, the upgradle.yml will get loaded from classpath")
                exitProcess(1)
            }
        }

        fun Boolean.then(block: () -> Unit): Boolean {
            if (this) {
                block()
            }
            return this
        }

        @OptIn(ExperimentalTime::class)
        suspend fun <T> retry(
            times: Int = 10,
            wait: Duration = DEFAULT_WAIT,
            body: () -> T
        ): T = (1..times)
            .map { id ->
                runCatching(body).getOrElse { error ->
                    if (id < times) {
                        logger.error("An error occurred at attempt $id/$times, waiting $wait before retrying", error)
                        delay(wait)
                    } else {
                        logger.error("An error occurred at attempt $id/$times. No further retries")
                        throw error
                    }
                    null
                }
            }.filterNotNull().first()

        fun prepareRepository(git: Git, branch: Branch, update: Operation): Boolean =
            git.branchList().call().none { it.name == branch.name }.then {
                logger.info("Checking out ${branch.name}")
                git.checkout().setName(branch.name).call()
                logger.info("Resetting the repo status")
                git.reset().setMode(ResetCommand.ResetType.HARD).call()
                // Start a new working branch
                git.checkout().setCreateBranch(true).setName(update.branch).call()
            }

        @ExperimentalPathApi
        @ExperimentalCoroutinesApi
        @FlowPreview
        @JvmStatic
        fun main(args: Array<String>) {
            val upgradle: UpGradle = upgradleFromArguments(args)
            val config = upgradle.configuration
            val credentials = Credentials.loadGitHubCredentials()
            val modules = config.modules.map { it.asModule }
            runBlocking(Dispatchers.Default) {
                GraphqlSource(FuelGithubClient(credentials))
                    .getMatching(Selector(includes = config.includes, excludes = config.excludes.orEmpty()))
                    .flatMapMerge { (repository, branch) ->
                        modules.asFlow().map { module ->
                            launch {
                                upgradle.runModule(repository, branch, module, credentials)
                            }
                        }
                    }
                    .buffer()
                    .collect()
            }
            logger.info("Done")
        }
    }
}
