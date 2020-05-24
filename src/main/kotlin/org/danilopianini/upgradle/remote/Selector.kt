package org.danilopianini.upgradle.remote

import org.danilopianini.upgradle.config.RepoDescriptor

data class Selector(val includes: Set<RepoDescriptor>, val excludes: Set<RepoDescriptor>) {
    fun <T> applyTo(value: T, via: RepoDescriptor.(T) -> Boolean): Boolean =
        includes.any { it.via(value) } && excludes.none { it.via(value) }
}
