package org.danilopianini.upgradle.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.runBlocking
import org.danilopianini.upgradle.config.RepoDescriptor
import org.danilopianini.upgradle.remote.graphql.GithubGraphqlClient

class GraphqlSource(private val client: GithubGraphqlClient) : BranchSource {
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun getMatching(selector: Selector): Set<BranchSource.SelectedRemoteBranch> = runBlocking {
        client.repositories()
            .filter { repo -> selector.applyTo(repo, RepoDescriptor::matches) }
            .buffer(50)
            .flatMapMerge { repo ->
                client.topics(repo)
                    .filter { topic -> selector.applyTo(topic, RepoDescriptor::matches) }
                    .map { repo }
                    .take(1)
            }
            .flatMapMerge { repo ->
                client.branches(repo)
                    .filter { branch -> selector.applyTo(branch, RepoDescriptor::matches) }
                    .map { BranchSource.SelectedRemoteBranch(repo, it) }
            }
            .toCollection(mutableSetOf())
    }
}
