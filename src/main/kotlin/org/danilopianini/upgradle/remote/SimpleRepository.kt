package org.danilopianini.upgradle.remote

interface Repository {
    val owner: String
    val name: String
    val cloneUri: String
}

data class SimpleRepository(
    override val owner: String,
    override val name: String,
    override val cloneUri: String
) : Repository
