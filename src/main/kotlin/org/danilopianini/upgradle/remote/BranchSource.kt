package org.danilopianini.upgradle.remote

import org.danilopianini.upgradle.config.RepoDescriptor
import org.danilopianini.upgradle.config.SelectedRemoteBranch
import org.eclipse.egit.github.core.service.RepositoryService
import java.util.stream.Collectors

interface BranchSource {
    fun getMatching(includes: Set<RepoDescriptor>, excludes: Set<RepoDescriptor>): Set<SelectedRemoteBranch>
}

class FilteringBranchSource(private val service: RepositoryService) : BranchSource {
    override fun getMatching(includes: Set<RepoDescriptor>, excludes: Set<RepoDescriptor>): Set<SelectedRemoteBranch> =
        service.repositories.parallelStream()
            .filter { repo ->
                includes.any { it matches repo } && excludes.none { it matches repo }
            }
            .flatMap { repo ->
                service.getBranches(repo).parallelStream()
                    .filter { branch ->
                        includes.any { it matches branch } && excludes.none { it matches branch }
                    }
                    .map { SelectedRemoteBranch(repo, it) }
            }
            .collect(Collectors.toSet())
}
