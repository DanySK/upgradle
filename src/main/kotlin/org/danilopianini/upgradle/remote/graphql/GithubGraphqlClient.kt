package org.danilopianini.upgradle.remote.graphql

import kotlinx.coroutines.flow.Flow
import org.danilopianini.upgradle.remote.Repository

interface GithubGraphqlClient {
    fun repositories(): Flow<RemoteRepository>
    fun topicsOf(owner: String, name: String): Flow<String>
    fun branchesOf(repository: Repository): Flow<String>
}
