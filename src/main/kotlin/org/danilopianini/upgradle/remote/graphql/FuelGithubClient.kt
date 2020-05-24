package org.danilopianini.upgradle.remote.graphql

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.github.kittinunf.fuel.gson.jsonBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import org.danilopianini.upgradle.api.Credentials
import org.danilopianini.upgradle.api.Token
import org.danilopianini.upgradle.api.UserAndPassword
import org.danilopianini.upgradle.remote.Repository
import java.io.Reader
import java.util.Base64

class FuelGithubClient(credentials: Credentials) : GithubGraphqlClient {
    private companion object {
        val classLoader: ClassLoader by lazy { this::class.java.classLoader }
        const val ENDPOINT = "https://api.github.com/graphql"
    }

    private val authorization = when (credentials) {
        is Token -> "Bearer ${credentials.token}"
        is UserAndPassword -> {
            val enc = Base64.getEncoder()
                .encodeToString("${credentials.user}:${credentials.password}".toByteArray())

            "Basic $enc"
        }
    }

    private suspend fun getRequestBody(resourceName: String, variables: Map<String, Any?>): Map<String, Any> =
        withContext(Dispatchers.IO) {
            val query = requireNotNull(classLoader.getResourceAsStream(resourceName))
                .bufferedReader()
                .use(Reader::readText)
            mapOf("query" to query, "variables" to variables)
        }

    private suspend inline fun <reified T : Any> requestOf(resourceName: String, variables: Map<String, Any?>): T =
        Fuel.post(ENDPOINT)
            .header("Content-Type", "application/json")
            .header("Authorization", authorization)
            .jsonBody(getRequestBody(resourceName, variables))
            .awaitResponseResult(gsonDeserializerOf(T::class.java))
            .third
            .get()

    private suspend fun repoRequest(after: String?): UserRepositories =
        requestOf("UserRepositories.graphql", mapOf("after" to after))

    private suspend fun topicRequest(after: String?, repo: Repository): RepositoryDetails<TopicData> =
        requestOf(
            "RepositoryTopics.graphql",
            mapOf("after" to after, "owner" to repo.owner, "name" to repo.name)
        )

    private suspend fun branchRequest(after: String?, repo: Repository): RepositoryDetails<BranchData> =
        requestOf(
            "RepositoryBranches.graphql",
            mapOf("after" to after, "owner" to repo.owner, "name" to repo.name)
        )

    @FlowPreview
    override fun repositories(): Flow<RemoteRepository> =
        paginate(::repoRequest)
            .flatMapConcat { it.data.viewer.repositories.nodes.orEmpty().asFlow() }
            .filterNotNull()

    @FlowPreview
    override fun topicsOf(repository: Repository): Flow<String> =
        paginate { after -> topicRequest(after, repository) }
            .flatMapConcat { it.data.repository.info.nodes.orEmpty().asFlow() }
            .mapNotNull { it?.topic?.name }

    @FlowPreview
    override fun branchesOf(repository: Repository): Flow<String> =
        paginate { after -> branchRequest(after, repository) }
            .flatMapConcat { it.data.repository.info.nodes.orEmpty().asFlow() }
            .mapNotNull { it?.name }
}
