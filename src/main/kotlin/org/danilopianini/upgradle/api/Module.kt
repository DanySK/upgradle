package org.danilopianini.upgradle.api

import io.github.classgraph.ClassGraph
import org.danilopianini.upgradle.config.UpgradleModule
import java.io.File
import java.lang.reflect.Constructor

interface Module : (File) -> List<Operation> {
    val name: String
        get() = javaClass.simpleName

    companion object {
        private fun ClassGraph.blackListPackages(vararg packageNames: String) = rejectPackages(*packageNames)
        private val subclasses: List<Class<out Module>> by lazy {
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
        val UpgradleModule.asModule: Module get() = sequenceOf(Class<*>::getCanonicalName, Class<*>::getSimpleName)
            .flatMap { nameOf ->
                sequenceOf(false, true).flatMap { ignoreCase ->
                    subclasses.find { nameOf(it).equals(name, ignoreCase) }?.let { sequenceOf(it) } ?: emptySequence()
                }
            }
            .firstOrNull()
            ?.constructors
            ?.firstOrNull { it.parameterCount == 1 && Map::class.java.isAssignableFrom(it.parameterTypes[0]) }
            ?.let { it as Constructor<out Module> }
            ?.newInstance(options)
            ?: throw IllegalStateException("No module available for $this")
    }
}
