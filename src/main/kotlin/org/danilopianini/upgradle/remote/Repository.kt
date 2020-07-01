package org.danilopianini.upgradle.remote

interface Repository {
    val owner: String
    val name: String
    val cloneUri: String
    val topics: List<String>
}
