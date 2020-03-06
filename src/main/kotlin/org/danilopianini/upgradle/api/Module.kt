package org.danilopianini.upgradle.api

import arrow.core.ListK
import arrow.core.extensions.listk.applicative.applicative
import arrow.core.extensions.listk.foldable.firstOption
import arrow.core.k
import arrow.core.orElse
import io.github.classgraph.ClassGraph
import org.eclipse.jgit.util.StringUtils
import kotlin.reflect.KClass

interface Module {
    val name: String
        get() = javaClass.simpleName

    operator fun invoke(parameters: Map<String, Any> = emptyMap())

    object StringExtensions {
        val subclasses by lazy {
            ClassGraph()
                .blacklistPackages("java", "javax")
                .enableAllInfo()
                .scan()
                .getSubclasses(Module::class.java.canonicalName)
                .loadClasses()
        }
        val String.asUpGradleModule get() =
            ListK.applicative()
                .map(
                    // Cartesian product of:
                    // Methods for extracting possible names
                    listOf(Class<*>::getCanonicalName, Class<*>::getSimpleName).k(),
                    // Whether to ignore case
                    listOf(false, true).k()
                ) { (name, withCase) ->
                    subclasses.find { equals(name.invoke(it), ignoreCase = withCase) }
                }
                .firstOption()
                .orElse {
                    throw IllegalStateException("No module available for $this")
                }
    }

}