package org.danilopianini.upgradle

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import com.uchuhimo.konf.source.yaml
import org.danilopianini.upgradle.api.Credentials
import org.danilopianini.upgradle.api.Credentials.Companion.authenticated
import org.danilopianini.upgradle.api.Module.StringExtensions.asUpGradleModule
import org.danilopianini.upgradle.api.OnFile
import org.danilopianini.upgradle.api.Pattern
import org.danilopianini.upgradle.config.Configurator
import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.PullRequestMarker
import org.eclipse.egit.github.core.client.RequestException
import org.eclipse.egit.github.core.service.PullRequestService
import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import java.io.File
import kotlin.system.exitProcess

class UpGradle(configuration: Config.() -> Config = { from.yaml.resource("upgradle.yml") }) {
    val configuration = Configurator.load(configuration)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val upgradle: UpGradle = when (args.size) {
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
            val credentials = Credentials.loadGitHubCredentials()
            val repositoryService = RepositoryService().authenticated(credentials)
            upgradle.configuration.selectedRemoteBranchesFor(repositoryService).forEach { (repository, branch) ->
                upgradle.configuration.modules.map { it.asUpGradleModule }.parallelStream().forEach { module ->
                    println("Running ${module.name} on ${repository.owner.login}/${repository.name} on branch ${branch.name}")
                    val destination =
                        createTempDir("upgradle-${repository.owner.login}_${repository.name}_${branch.name}_${module.name}")
                    val git = Git.cloneRepository()
                        .authenticated(credentials)
                        .setURI(repository.htmlUrl)
                        .setBranch(branch.name)
                        .setDirectory(destination)
                        .call()
                    module.operationsFor(destination).forEach { update ->
                        // Checkout a clean starting branch
                        git.checkout().setName(branch.name)
                        git.reset().setMode(ResetCommand.ResetType.HARD).call()
                        // Start a new working branch
                        git.checkout().setCreateBranch(true).setName(update.branch).call()
                        // Run the update operation
                        val add = git.add()
                        update().forEach {
                            // Add changes to the tracker
                            add.addFilepattern(
                                when (it) {
                                    is OnFile -> it.file.relativeTo(destination).path
                                    is Pattern -> it.pattern.replace(destination.absolutePath, "")
                                }
                            )
                        }
                        add.call()
                        // Commit changes
                        git.commit()
                            .setMessage(update.commitMessage)
                            .setAuthor(PersonIdent("UpGradle [Bot]", "danilo.pianini@gmail.com"))
                            .call()
                        // Push the new branch
                        val pushResults = git.push()
                            .authenticated(credentials)
                            .setForce(false)
                            .setRemote("origin")
                            .setRefSpecs(RefSpec(update.branch))
                            .call()
                            .flatMap { it.remoteUpdates }
                        if (pushResults.all { it.status == RemoteRefUpdate.Status.OK }) {
                            val prService = PullRequestService().authenticated(credentials)
                            val head = PullRequestMarker()
                                    .setRef(update.branch)
                                    .setLabel(update.branch)
                            val base = PullRequestMarker()
                                    .setRef(branch.name)
                                    .setLabel(branch.name)
                            val pr = PullRequest()
                                    .setBase(base)
                                    .setHead(head)
                                    .setTitle(update.pullRequestTitle)
                                    .setBody(update.pullRequestMessage)
                                    .setBodyText(update.pullRequestMessage)
                            try {
                                prService.createPullRequest(repository, pr)
                            } catch (requestException: RequestException) {
                                when (requestException.status) {
                                    422 -> println(requestException.message)
                                    else -> throw requestException
                                }
                            }
                        } else {
                            println("Push failed:")
                            pushResults.forEach(::println)
                        }
                    }
                    destination.deleteRecursively()
                }
            }
        }
    }
}
