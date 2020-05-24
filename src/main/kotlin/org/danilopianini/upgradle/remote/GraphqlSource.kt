package org.danilopianini.upgradle.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.runBlocking
import org.danilopianini.upgradle.config.RepoDescriptor
import org.danilopianini.upgradle.remote.graphql.GithubGraphqlClient
import org.danilopianini.upgradle.remote.graphql.RemoteRepository
import org.slf4j.LoggerFactory

class GraphqlSource(private val client: GithubGraphqlClient) : BranchSource {
    private val logger = LoggerFactory.getLogger(javaClass)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun getMatching(selector: Selector): Set<BranchSource.SelectedRemoteBranch> = runBlocking {
        client.repositories()
            .filter { it.isWritable() }
            .map { GithubRepository(it) }
            .filter { repo -> selector.applyTo(repo, RepoDescriptor::matches) }
            .buffer(200)
            .flatMapMerge { repo ->
                flow { if (hasMatchingTopic(repo, selector)) emit(repo) }
            }
            .flatMapMerge { repo -> matchingBranches(repo, selector) }
            .toCollection(mutableSetOf()).also {
                logger.info("Found ${it.size} matching configs")
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun hasMatchingTopic(repository: Repository, selector: Selector): Boolean =
        client.topicsOf(repository)
            // Repos without topics would be filtered even without topic rule
            .onStart { emit("") }
            .filter { topic -> selector.applyTo(topic, RepoDescriptor::matches) }
            .count() > 0

    private fun matchingBranches(repository: Repository, selector: Selector): Flow<BranchSource.SelectedRemoteBranch> =
        client.branchesOf(repository)
            .map { GithubBranch(it) }
            .filter { branch -> selector.applyTo(branch, RepoDescriptor::matches) }
            .map { BranchSource.SelectedRemoteBranch(repository, it) }

    private fun RemoteRepository.isWritable() =
        (viewerPermission == "WRITE" || viewerPermission == "ADMIN") &&
                !(isArchived || isDisabled || isMirror)
}

private inline class GithubRepository(private val underlying: RemoteRepository) : Repository {
    override val owner: String
        get() = underlying.owner.login
    override val name: String
        get() = underlying.name
    override val cloneUri: String
        get() = underlying.url
}

private inline class GithubBranch(override val name: String) : Branch
