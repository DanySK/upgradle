package org.danilopianini.upgradle.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.service.RepositoryService

data class GitHubAccess(val token: String? = null, val user: String? = null, val password: String? = null) {
    init {
        if (token == null) {
            require(user != null) {
                "Either an accessToken or a userName must be provided."
            }
            require(password != null) {
                "If no accessToken is provided, then a password is mandatory"
            }
        } else {
            require(password == null) {
                "You must not provide both accessToken and password."
            }
        }
    }
}

data class SelectedRemoteBranch(val repository: Repository, val branch: String)

data class RepoDescriptor(val user: List<String>, val repo: List<String>, val branch: List<String> = listOf(".*")) {
    val asRegexFilter by lazy { Regex("""${user}\/${repo}""") }

}

data class Configuration(val include: List<RepoDescriptor>, val exclude: List<RepoDescriptor>, val modules: List<String>) {

    fun selectedRemoteBranchesFor(service: RepositoryService): Set<SelectedRemoteBranch> = service.repositories.asSequence()
        .flatMap { repo ->
            // For each name match, add the required branches
            include
                .filter { repo.name.matches(it.asRegexFilter) }
                .flatMap { descriptor -> descriptor.branch.map { SelectedRemoteBranch(repo, it) } }
                .asSequence()
        }
        .filterNot { (repository, branch) ->
            exclude.any { repository.name.matches(it.asRegexFilter) && it.branch.contains(branch) }
        }
        .toSet()

}

object Configurator {
    fun load(body: Config.() -> Config): Configuration = Config().body()
            .at("").toValue()
}



fun main() {
    val result = Configurator.load {
        from.yaml.resource("baseconfig2.yml")
    }
    println(result)
}