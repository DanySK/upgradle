package org.danilopianini.upgradle.remote

interface BranchSource {
    data class SelectedRemoteBranch(val repository: Repository, val branch: Branch) {
        override fun toString(): String = "${repository.owner}/${repository.name}@${branch.name}"
    }

    fun getMatching(selector: Selector): Set<SelectedRemoteBranch>
}
