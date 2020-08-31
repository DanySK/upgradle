package org.danilopianini.upgradle.api

import arrow.core.ListK
import arrow.core.extensions.listk.applicative.applicative
import arrow.core.fix
import arrow.core.k
import io.github.classgraph.ClassGraph
import org.danilopianini.upgradle.config.UpgradleModule
import java.io.File
import java.lang.reflect.Constructor

interface Module : (File) -> List<Operation> {
    val name: String
        get() = javaClass.simpleName

    companion object {
        private fun ClassGraph.blackListPackages(vararg packageNames: String) = rejectPackages(*packageNames)
        private val subclasses by lazy {
            ClassGraph()
                .blackListPackages("java", "javax")
                .enableAllInfo()
                .scan()
                .getClassesImplementing(Module::class.java.canonicalName)
                .filter { !it.isAbstract }
                .loadClasses()
                .filterIsInstance<Class<out Module>>()
        }

        @Suppress("UNCHECKED_CAST")
        val UpgradleModule.asModule: Module get() = ListK.applicative()
            .mapN(
                // Cartesian product of:
                // Methods for extracting possible names
                listOf(Class<*>::getCanonicalName, Class<*>::getSimpleName).k(),
                // Whether to ignore case
                listOf(false, true).k()
            ) { (classNameOf, withCase) ->
                subclasses.find { name.equals(classNameOf(it), ignoreCase = withCase) }
            }
            .fix()
            .filterNotNull()
            .firstOrNull()
            ?.constructors
            ?.filter { it.parameterCount == 1 && Map::class.java.isAssignableFrom(it.parameterTypes[0]) }
            ?.firstOrNull()
            ?.let { it as Constructor<out Module> }
            ?.newInstance(options)
            ?: throw IllegalStateException("No module available for $this")
    }
}
