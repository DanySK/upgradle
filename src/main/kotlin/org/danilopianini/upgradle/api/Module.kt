package org.danilopianini.upgradle.api

interface Module {
    val name: String
        get() = javaClass.simpleName

    operator fun invoke(parameters: Map<String, Any> = emptyMap())
}
