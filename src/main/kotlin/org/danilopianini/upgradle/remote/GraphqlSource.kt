package org.danilopianini.upgradle.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.danilopianini.upgradle.remote.graphql.GithubGraphqlClient
import org.danilopianini.upgradle.remote.graphql.RemoteRepository
import org.slf4j.LoggerFactory

class GraphqlSource(private val client: GithubGraphqlClient) : BranchSource {
    private val logger = LoggerFactory.getLogger(javaClass)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun getMatching(selector: Selector): Flow<BranchSource.SelectedRemoteBranch> = runBlocking {
        client.repositories()
            .filter { it.isWritable() }
            .map { GithubRepository(it, client.topicsOf(it.owner.login, it.name).toList(ArrayList())) }
            .buffer()
            .flatMapMerge { repo ->
                client.branchesOf(repo).map { BranchSource.SelectedRemoteBranch(repo, GithubBranch(it)) }
            }
            .filter { branch ->
                selector.selects(branch).also { if (!it) logger.debug("Discarded: $branch") }
            }
            .onEach { logger.debug("Selected: $it") }
    }

    private fun RemoteRepository.isWritable() =
        (viewerPermission == "WRITE" || viewerPermission == "ADMIN") && !(isArchived || isDisabled || isMirror)

    private inner class GithubRepository(
        private val underlying: RemoteRepository,
        override val topics: List<String>
    ) : Repository {
        override val owner: String
            get() = underlying.owner.login
        override val name: String
            get() = underlying.name
        override val cloneUri: String
            get() = underlying.url
    }
}

private inline class GithubBranch(override val name: String) : Branch
