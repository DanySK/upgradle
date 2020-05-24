package org.danilopianini.upgradle.remote

import org.danilopianini.upgradle.config.RepoDescriptor

interface BranchSource {
    data class SelectedRemoteBranch(val repository: Repository, val branch: Branch) {
        override fun toString(): String = "${repository.owner}/${repository.name}@${branch.name}"
    }

    fun getMatching(includes: Set<RepoDescriptor>, excludes: Set<RepoDescriptor>?) =
        getMatching(Selector(includes, excludes.orEmpty()))

    fun getMatching(selector: Selector): Set<SelectedRemoteBranch>
}
