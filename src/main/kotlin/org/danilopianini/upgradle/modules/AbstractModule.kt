package org.danilopianini.upgradle.modules

import org.danilopianini.upgradle.api.Module
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

abstract class AbstractModule(options: Map<String, Any> = emptyMap()) : Module {
    protected val validVersionRegex: Regex
    protected val strategies: List<String>
    protected val logger = LoggerFactory.getLogger(this::class.java)

    init {
        val versionRegex: String by options.withDefault { ".*" }
        validVersionRegex = versionRegex.toRegex()
        strategies = options.getOrElse("strategy") { "latest" }
            .let {
                when (it) {
                    is String -> listOf(it)
                    is List<*> -> it.map { it.toString() }
                    is Map<*, *> -> {
                        logger.warn("Invalid options for RefreshVersions: $it, considering keys")
                        it.keys.map { it.toString() }
                    }
                    else -> {
                        val error = "Invalid options for RefreshVersions: $it"
                        logger.error(error)
                        throw IllegalStateException(error)
                    }
                }
            }
    }

    private val includesAll = all.isASelectedStrategy
    private val includesNext = next.isASelectedStrategy
    private val includesLatest = latest.isASelectedStrategy

    init {
        require(includesAll || includesNext || includesLatest) {
            "Inconsistent configuration for ${this::class.simpleName}, no version can be selected with options $options"
        }
    }

    /**
     * Applies a filtering [strategy] to a sequence.
     * If [strategy] is "next", takes the first element.
     * If [strategy] is "latest", takes the last element.
     * Otherwise, it returns the list as-is
     */
    fun <T : Any> Sequence<T>.filterByStrategy(): Sequence<T> =
        if (all.isASelectedStrategy) {
            this
        } else {
            sequenceOf(
                this.lastOrNull()?.takeIf { latest.isASelectedStrategy },
                this.firstOrNull()?.takeIf { next.isASelectedStrategy },
            ).filterNotNull().distinct()
        }

    private val List<String>.isASelectedStrategy get() = any { strategies.contains(it) }

    private companion object {
        private val next = "next".allVariants
        private val latest = "latest".allVariants
        private val all = "all".allVariants

        private val String.allVariants
            get() = listOf(
                this,
                this.toLowerCase(),
                this.toUpperCase(),
                this.capitalize()
            ).distinct()
    }
}
