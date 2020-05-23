package org.danilopianini.upgradle.remote

import org.danilopianini.upgradle.config.RepoDescriptor

interface BranchSource {
    data class SelectedRemoteBranch(val repository: Repository, val branch: Branch)

    fun getMatching(includes: Set<RepoDescriptor>, excludes: Set<RepoDescriptor>): Set<SelectedRemoteBranch>
}
