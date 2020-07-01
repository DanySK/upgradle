package org.danilopianini.upgradle.remote

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.danilopianini.upgradle.config.RepoDescriptor

internal class TestSelector : FreeSpec({
    "A Selector" - {
        val match = mock<RepoDescriptor> {
            on { matches(any()) } doReturn true
        }
        val noMatch = mock<RepoDescriptor> {
            on { matches(any()) } doReturn false
        }
        infix fun Selector.shouldEvalTo(expected: Boolean) =
            selects(mock()) shouldBe expected

        "without includes should match no input" - {
            Selector(includes = emptySet(), excludes = emptySet()) shouldEvalTo false
            Selector(includes = emptySet(), excludes = setOf(noMatch)) shouldEvalTo false
        }
        "without excludes" - {
            fun including(vararg desc: RepoDescriptor) = Selector(desc.toSet(), excludes = emptySet())
            "should match input if" - {
                "a single include descriptor matches" {
                    including(match, noMatch) shouldEvalTo true
                }
                "all include descriptors match" {
                    including(match, match) shouldEvalTo true
                }
            }
            "should not match input when no include descriptors match" {
                including(noMatch) shouldEvalTo false
            }
        }
        "with both includes and excludes" - {
            "should match input if no exclude descriptor matches and" - {
                fun including(vararg desc: RepoDescriptor) = Selector(desc.toSet(), excludes = setOf(noMatch))
                "a single include descriptor matches" {
                    including(match) shouldEvalTo true
                }
                "all include descriptors match" {
                    including(match, match) shouldEvalTo true
                }
            }
            "should not match input if" - {
                "no include descriptor matches" {
                    Selector(setOf(noMatch), setOf(noMatch)) shouldEvalTo false
                }
                "any include matches but at least one exclude descriptor also matches" {
                    Selector(setOf(match), setOf(match)) shouldEvalTo false
                    Selector(setOf(match), setOf(noMatch, match)) shouldEvalTo false
                }
            }
        }
    }
})
