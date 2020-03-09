package org.danilopianini.upgradle.api

import java.io.File

interface Operation: ()->List<Change> {

    val branch: String
    val commitMessage: String
    val pullRequestTitle: String
    val pullRequestMessage: String

}

class SimpleOperation(
    override val branch: String,
    override val commitMessage: String,
    override val pullRequestTitle: String,
    override val pullRequestMessage: String,
    private val operation: ()->List<Change>
) : Operation {
    override fun invoke(): List<Change> = operation()
}

sealed class Change
data class OnFile(val file: File): Change()
data class Pattern(val pattern: String): Change()