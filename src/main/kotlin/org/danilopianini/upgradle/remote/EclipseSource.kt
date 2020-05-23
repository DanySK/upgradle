package org.danilopianini.upgradle.remote

import org.danilopianini.upgradle.config.RepoDescriptor
import org.eclipse.egit.github.core.IRepositoryIdProvider
import org.eclipse.egit.github.core.Repository as GRepository
import org.eclipse.egit.github.core.RepositoryBranch
import org.eclipse.egit.github.core.service.RepositoryService
import org.danilopianini.upgradle.remote.BranchSource.SelectedRemoteBranch
import org.slf4j.LoggerFactory
import java.util.stream.Collectors
import java.util.stream.Stream

class EclipseSource(private val service: RepositoryService) : BranchSource {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getMatching(selector: Selector): Set<SelectedRemoteBranch> =
        service.repositories.parallelStream()
            .map(::EGitRepository)
            .onEach { logger.info("Found ${it.generateId()}") }
            .filter { repo -> selector.applyTo(repo, RepoDescriptor::matches) }
            .flatMap { repo ->
                service.getBranches(repo).parallelStream()
                    .map(::EGitBranch)
                    .filter { branch -> selector.applyTo(branch, RepoDescriptor::matches) }
                    .map { SelectedRemoteBranch(repo, it) }
            }
            .collect(Collectors.toSet()).also {
                logger.info("Found ${it.size} matching repositories.")
            }
}

private inline fun <T>Stream<T>.onEach(crossinline f: (T) -> Unit): Stream<T> =
    map { it.also(f) }

private inline class EGitBranch(private val underlying: RepositoryBranch) : Branch {
    override val name: String get() = underlying.name
}

private inline class EGitRepository(private val underlying: GRepository) : Repository, IRepositoryIdProvider {
    override fun generateId(): String = underlying.generateId()

    override val owner: String get() = underlying.owner.login
    override val name: String get() = underlying.name
    override val cloneUri: String get() = underlying.htmlUrl
}
