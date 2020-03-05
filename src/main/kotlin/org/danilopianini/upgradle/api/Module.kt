package org.danilopianini.upgradle.api

import io.github.classgraph.ClassGraph
import org.eclipse.jgit.util.StringUtils

interface Module {
    val name: String
        get() = javaClass.simpleName

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
            subclasses.find { equals(it.canonicalName, ignoreCase = false) }
                ?: subclasses.find { equals(it.canonicalName, ignoreCase = true) }
                ?: subclasses.find { equals(it.simpleName, ignoreCase = false) }
                ?: subclasses.find { equals(it.simpleName, ignoreCase = true) }
        }
    }

    operator fun invoke(parameters: Map<String, Any> = emptyMap())
}

