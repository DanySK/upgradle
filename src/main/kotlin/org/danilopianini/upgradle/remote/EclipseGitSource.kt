package org.danilopianini.upgradle.remote

import org.danilopianini.upgradle.config.RepoDescriptor
import org.eclipse.egit.github.core.IRepositoryIdProvider
import org.eclipse.egit.github.core.Repository as GRepository
import org.eclipse.egit.github.core.RepositoryBranch
import org.eclipse.egit.github.core.service.RepositoryService
import org.danilopianini.upgradle.remote.BranchSource.SelectedRemoteBranch
import java.util.stream.Collectors

class EclipseGitSource(private val service: RepositoryService) : BranchSource {
    override fun getMatching(includes: Set<RepoDescriptor>, excludes: Set<RepoDescriptor>): Set<SelectedRemoteBranch> =
        service.repositories.parallelStream()
            .map(::EGitRepository)
            .filter { repo ->
                includes.any { it matches repo } && excludes.none { it matches repo }
            }
            .flatMap { repo ->
                service.getBranches(repo).parallelStream()
                    .map(::EGitBranch)
                    .filter { branch ->
                        includes.any { it matches branch } && excludes.none { it matches branch }
                    }
                    .map { SelectedRemoteBranch(repo, it) }
            }
            .collect(Collectors.toSet())
}

private inline class EGitBranch(private val underlying: RepositoryBranch) : Branch {
    override val name: String get() = underlying.name
}

private inline class EGitRepository(private val underlying: GRepository) : Repository, IRepositoryIdProvider {
    override fun generateId(): String = underlying.generateId()

    override val owner: String get() = underlying.owner.login
    override val name: String get() = underlying.name
    override val cloneUri: String get() = underlying.htmlUrl
}
