package org.danilopianini.upgradle.api

import java.io.File

interface Operation {

    val branch: String
    val commitMessage: String
    val pullRequestTitle: String
    val pullRequestMessage: String

    operator fun invoke(destination: File): List<String>

}

class SimpleOperation(
    override val branch: String,
    override val commitMessage: String,
    override val pullRequestTitle: String,
    override val pullRequestMessage: String,
    private val invoke: File.()->List<String>
) : Operation {
    override fun invoke(destination: File): List<String> = destination.invoke()
}
