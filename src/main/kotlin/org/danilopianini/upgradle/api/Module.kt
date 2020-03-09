package org.danilopianini.upgradle.api

import arrow.core.ListK
import arrow.core.extensions.listk.applicative.applicative
import arrow.core.fix
import arrow.core.k
import io.github.classgraph.ClassGraph
import java.io.File

interface Module {
    val name: String
        get() = javaClass.simpleName

    fun operationsFor(localDirectory: File): List<Operation>

    object StringExtensions {
        val subclasses by lazy {
            ClassGraph()
                .blacklistPackages("java", "javax")
                .enableAllInfo()
                .scan()
                .getClassesImplementing(Module::class.java.canonicalName)
                .filter { !it.isAbstract }
                .loadClasses()
                .map { it as Class<out Module> }
        }

        val String.asUpGradleModule: Module get() =
            ListK.applicative()
                .map(
                    // Cartesian product of:
                    // Methods for extracting possible names
                    listOf(Class<*>::getCanonicalName, Class<*>::getSimpleName).k(),
                    // Whether to ignore case
                    listOf(false, true).k()
                ) { (name, withCase) ->
                    subclasses.find { equals(name.invoke(it), ignoreCase = withCase) }
                }.fix()
                .filterNotNull()
                .first()?.getConstructor()?.newInstance()
                ?: throw IllegalStateException("No module available for $this")
    }

}