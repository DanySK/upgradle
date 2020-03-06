package org.danilopianini.upgradle

import com.google.gson.Gson
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import com.uchuhimo.konf.source.yaml
import org.danilopianini.upgradle.api.Credentials
import org.danilopianini.upgradle.api.Credentials.Companion.authenticated
import org.danilopianini.upgradle.api.Module.StringExtensions.asUpGradleModule
import org.danilopianini.upgradle.config.Configurator
import org.eclipse.egit.github.core.PullRequest
import org.eclipse.egit.github.core.PullRequestMarker
import org.eclipse.egit.github.core.service.PullRequestService
import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.net.URL
import kotlin.system.exitProcess

class UpGradle(configuration: Config.()->Config = {from.yaml.resource("upgradle.yml")}) {
    val configuration = Configurator.load(configuration)

    fun main(vararg args: String) {
        val upgradle: UpGradle = when(args.size) {
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
                    when(file.extension) {
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
            upgradle.configuration.modules.map { it.asUpGradleModule }.forEach { module ->
                val destination = createTempDir("upgradle")
//                val local = Git.cloneRepository()
//                    .setCredentialsProvider(credentials)
//                    .setURI(repository.htmlUrl)
//                    .setDirectory(destination)
//                    .call()
            }
        }
    }
}

fun main() {
    val username = "DanySK"
    val password = "********"
    val gson = Gson()
    val service = RepositoryService()
    val gradleRepo = service.getRepository("gradle", "gradle")
    val response = URL("https://api.github.com/repos/gradle/gradle/releases").readText()
    println(response)
    val releases = gson.fromJson(response, List::class.java)
    val latestGradle = releases.asSequence()
        .map { it as Map<String, String> }
        .map { it["name"] }
        .filterNotNull()
        .first { it.matches(Regex("""\d+\.\d+(\.\d+)?""")) }
    println(latestGradle)
    service.client.setCredentials(username, password)
    println(service.repositories.filter { it.isPrivate }.map { it.name })
    val sandbox = service.repositories.filter { it.name.contains("playground") }.first()
    println(sandbox.name)
    val destination = createTempDir("upgradle")
    println("Destination: $destination")
    val credentials = UsernamePasswordCredentialsProvider(username, password)
    val gitRepo = Git.cloneRepository()
        .setCredentialsProvider(credentials)
        .setURI(sandbox.htmlUrl)
        .setDirectory(destination)
        .call()
    val branch = gitRepo.repository.branch
    val properties = File("$destination/gradle/wrapper/gradle-wrapper.properties")
    if (properties.exists() && properties.isFile) {
        val oldProperties = properties.readText()
        val newProperties = oldProperties.replace(Regex("gradle-.*.zip"), "gradle-$latestGradle-bin.zip")
        println(oldProperties)
        println(newProperties)
        if (oldProperties != newProperties) {
            val newBranch = "upgradle-bump-gradle-to-$latestGradle"
            gitRepo.checkout().setCreateBranch(true).setName("upgradle-bump-gradle-to-$latestGradle").call()
            properties.writeText(newProperties)
            gitRepo.add().addFilepattern("gradle/wrapper/").call()
            val message = "Bump Gradle wrapper to $latestGradle"
            gitRepo.commit()
                .setMessage(message)
                .setAuthor(PersonIdent("Danilo Pianini", "danilo.pianini@gmail.com"))
                .call()
            val pushResult = gitRepo.push().setCredentialsProvider(credentials)
                .setRemote("origin")
                .setRefSpecs(RefSpec(newBranch))
                .call()
            println(pushResult)
            val prService = PullRequestService()
            prService.client.setCredentials(username, password)
            val head = PullRequestMarker()
                .setRef(newBranch)
                .setLabel(newBranch)
            val base = PullRequestMarker()
                .setRef(branch)
                .setLabel(branch)
            val pr = PullRequest()
                .setBase(base)
                .setHead(head)
                .setTitle(message)
                .setBodyText("Courtesy of org.danilopianini.upgradle.UpGradle")
            prService.createPullRequest(sandbox, pr)
        }
    }
}