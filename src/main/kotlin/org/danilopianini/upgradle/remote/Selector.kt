package org.danilopianini.upgradle.remote

import org.danilopianini.upgradle.config.RepoDescriptor

data class Selector(val includes: Set<RepoDescriptor>, val excludes: Set<RepoDescriptor>) {

    fun selects(remoteBranch: BranchSource.SelectedRemoteBranch): Boolean =
        includes.any { it.matches(remoteBranch) } && excludes.none { it.matches(remoteBranch) }
}
