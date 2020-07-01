package org.danilopianini.upgradle.remote

interface BranchSource {
    data class SelectedRemoteBranch(val repository: Repository, val branch: Branch) {
        override fun toString(): String = "${branch.name}@${repository.owner}/${repository.name}"
    }

    fun getMatching(selector: Selector): Set<SelectedRemoteBranch>
}
