package org.danilopianini.upgradle.config

import arrow.core.ListK
import arrow.core.extensions.listk.applicative.applicative
import arrow.core.fix
import arrow.core.k
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import org.eclipse.egit.github.core.IRepositoryIdProvider
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.RepositoryBranch
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

data class SelectedRemoteBranch(val repository: Repository, val branch: RepositoryBranch)

data class RepoDescriptor(val owners: List<String>, val repos: List<String>, val branches: List<String> = listOf(".*")) {
    val ownersRegex by lazy { owners.toRegex() }
    val reposRegex by lazy { repos.toRegex() }
    val branchesRegex by lazy { branches.toRegex() }

    fun validBranchesFor(service: RepositoryService, repository: Repository): List<RepositoryBranch> =
        if (ownersRegex.any { it.matches(repository.owner.name) }
            && reposRegex.any { it.matches(repository.name) }
        ) {
            service.getBranches { repository.id.toString() }
                .filter { branch: RepositoryBranch -> branchesRegex.any{ it.matches(branch.name) } }
        } else {
            emptyList()
        }

    companion object {
        private fun List<String>.toRegex() = map { Regex(it) }
    }
}

data class Configuration(val includes: List<RepoDescriptor>, val excludes: List<RepoDescriptor>, val modules: List<String>) {

    fun selectedRemoteBranchesFor(service: RepositoryService): Set<SelectedRemoteBranch> =
        service.repositories.parallelStream()
            .flatMap { remote ->
                includes.map { it.validBranchesFor(service, remote) }
                    .distinctBy { it.name }
            }
//        .filter {
//            it.owner.name.matches()
//        }
//        .flatMap { remote ->
//            // For each name match, add the required branches
//            includes
//                .filter { include ->
//                    include.asRegexFilters.any { remote.name.matches(it) }
//                }
//                .flatMap { descriptor ->
//                    service.getBranches { remote.id.toString() }
//                        .filter { branch -> descriptor.branches.any { branch.name.matches(Regex(it)) } }
//                        .map { SelectedRemoteBranch(remote, it) }
//                }
//                .asSequence()
//        }
//        .filterNot { (repository, branch) ->
//            excludes.any { repository.name.matches(it.asRegexFilter) && branch.name.matches(Regex()) }
//        }
//        .filter { (repository, branch) ->
//            service.getBranches(IRepositoryIdProvider { repository.id })
//        }
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