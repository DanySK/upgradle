package org.danilopianini.upgradle.remote.graphql

import kotlinx.coroutines.flow.Flow
import org.danilopianini.upgradle.remote.Branch
import org.danilopianini.upgradle.remote.Repository

interface GithubGraphqlClient {
    fun repositories(): Flow<WritableGithubRepository>
    fun topics(repository: WritableGithubRepository): Flow<String>
    fun branches(repository: WritableGithubRepository): Flow<Branch>

    data class WritableGithubRepository(
        override val owner: String,
        override val name: String,
        override val cloneUri: String
    ) : Repository
}
