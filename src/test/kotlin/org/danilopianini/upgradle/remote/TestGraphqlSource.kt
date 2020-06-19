package org.danilopianini.upgradle.remote

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.danilopianini.upgradle.remote.graphql.GithubGraphqlClient
import org.mockito.stubbing.OngoingStubbing

internal object TestGraphqlSource : FreeSpec({
    "The Graphql Source" - {
        "should match topics correctly, returning" - {
            "true" - {
                "when repo has matching topic" {
                    test(withTopics = flowOf("topic"), returning = true) {
                        doAnswer { it.arguments.first() == "topic" }
                    }
                }
                "when repo has no topics but no topic selector configured" {
                    test(withTopics = emptyFlow(), returning = true) { doReturn(true) }
                }
            }
            "false when repo has no matching topics" {
                test(withTopics = flowOf("topic"), returning = false) { doReturn(false) }
            }
        }
    }
})

private suspend inline fun test(
    withTopics: Flow<String>,
    returning: Boolean,
    selectorAction: OngoingStubbing<Boolean>.() -> Unit
) {
    val selector = mock<Selector> { on { applyTo(any<String>(), any()) }.selectorAction() }
    val client = mock<GithubGraphqlClient> { on { topicsOf(any()) } doReturn withTopics }
    val subject = GraphqlSource(client)
    subject.hasMatchingTopic(mock(), selector) shouldBe returning
}
