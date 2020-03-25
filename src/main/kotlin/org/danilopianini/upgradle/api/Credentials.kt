package org.danilopianini.upgradle.api

import org.eclipse.egit.github.core.service.GitHubService
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.lang.IllegalStateException

sealed class Credentials {
    companion object {
        fun loadGitHubCredentials(): Credentials {
            val validPrefixes = listOf("GITHUB_", "GH_", "RELEASES_")
            val suffixes = listOf("TOKEN", "USERNAME", "USER", "PASSWORD")
            val credentials: List<Credentials> = validPrefixes
                .flatMap { prefix -> suffixes.map { prefix + it } }
                .map { it to System.getenv(it) }
                .filter { (_, value) -> value != null }
                .groupBy { (name, _) -> name.endsWith("TOKEN") }
                .filterNot { (_, keys) -> keys.isEmpty() }
                .mapValues { it.value.toMap() }
                .map { (isToken, keys) ->
                    fun singleValueMatching(
                        filter: (String) -> Boolean = { true },
                        onErrorMessage: (Collection<String>) -> String
                    ): String {
                        val entries = keys.filterKeys(filter).values
                        require(entries.size == 1) { onErrorMessage(entries) }
                        return entries.first()
                    }
                    if (isToken) {
                        Token(singleValueMatching { "Conflicting token declarations: $keys" })
                    } else {
                        val user = singleValueMatching({ it.contains("USER") }) {
                            "Conflicting username declarations: $it"
                        }
                        val password = singleValueMatching({ it.endsWith("PASSWORD") }) {
                            "Multiple password definitions."
                        }
                        UserAndPassword(user, password)
                    }
                }
            if (credentials.size > 1) {
                println("WARNING: multiple credential definitions.")
            }
            return credentials.firstOrNull() ?: throw IllegalStateException("No token, username, or password provided")
        }

        fun <S : GitHubService> S.authenticated(credentials: Credentials): S = this.also {
            when (credentials) {
                is Token -> client.setOAuth2Token(credentials.token)
                is UserAndPassword -> client.setCredentials(credentials.user, credentials.password)
            }
        }

        fun <C : TransportCommand<*, *>> C.authenticated(credentials: Credentials): C = this.also {
            setCredentialsProvider(when (credentials) {
                is Token -> UsernamePasswordCredentialsProvider(credentials.token, "")
                is UserAndPassword -> UsernamePasswordCredentialsProvider(credentials.user, credentials.password)
            })
        }
    }
}
class Token(val token: String) : Credentials()
class UserAndPassword(val user: String, val password: String) : Credentials()
