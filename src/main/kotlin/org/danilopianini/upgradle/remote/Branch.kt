package org.danilopianini.upgradle.remote

interface Branch {
    val name: String
}

inline class SimpleBranch(override val name: String) : Branch
