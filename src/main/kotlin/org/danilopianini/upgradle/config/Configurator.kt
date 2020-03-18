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
import java.util.stream.Collectors

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

    fun validBranchesFor(service: RepositoryService, repository: Repository): List<RepositoryBranch> {
        val owner = repository.owner.login
        val name = repository.name
        if (ownersRegex.any { it.matches(owner) }
            && reposRegex.any { it.matches(name) }
        ) {
            return service.getBranches { "$owner/$name" }
                .filter { branch: RepositoryBranch -> branchesRegex.any{ it.matches(branch.name) } }
        } else {
            return  emptyList()
        }
    }

    companion object {
        private fun List<String>.toRegex() = map { Regex(it) }
    }
}

data class Configuration(val includes: List<RepoDescriptor>, val excludes: List<RepoDescriptor>, val modules: List<String>) {

    fun selectedRemoteBranchesFor(service: RepositoryService): Set<SelectedRemoteBranch> =
        service.repositories.parallelStream()
            .flatMap { remote ->
                includes.parallelStream()
                    .flatMap { it.validBranchesFor(service, remote).stream() }
                    .collect(Collectors.toList())
                    .distinctBy { it.name }
                    .stream()
                    .map { SelectedRemoteBranch(remote, it) }
            }
            .filter { (remote, branch) ->
                excludes.none { exclusion ->
                    exclusion.ownersRegex.any { it.matches(remote.owner.login) }
                        && exclusion.reposRegex.any { it.matches(remote.name) }
                        && exclusion.branchesRegex.any { it.matches(branch.name) }
                }
            }
            .collect(Collectors.toSet())
}

object Configurator {
    fun load(body: Config.() -> Config): Configuration = Config().body()
            .at("").toValue()
}
