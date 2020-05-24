package org.danilopianini.upgradle.remote

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.danilopianini.upgradle.remote.graphql.GithubGraphqlClient

internal class TestGraphqlSource : FreeSpec({
    val client = mockk<GithubGraphqlClient>()
    val selector = mockk<Selector>()
    val subject = GraphqlSource(client)

    "The Graphql Source" - {
        "should match topics correctly, returning" - {
            "true" - {
                "when repo has matching topic" {
                    every { selector.applyTo(any<String>(), any()) } answers { v ->
                        v.invocation.args.first() == "topic"
                    }
                    every { client.topicsOf(any()) } returns flowOf("topic")

                    subject.hasMatchingTopic(mockk(), selector) shouldBe true
                }
                "when repo has no topics but no topic selector configured" {
                    every { selector.applyTo(any<String>(), any()) } returns true
                    every { client.topicsOf(any()) } returns emptyFlow()

                    subject.hasMatchingTopic(mockk(), selector) shouldBe true
                }
            }
            "false when repo has no matching topics" {
                every { selector.applyTo(any<String>(), any()) } returns false
                every { client.topicsOf(any()) } returns flowOf("topic")

                subject.hasMatchingTopic(mockk(), selector) shouldBe false
            }
        }
    }
})
