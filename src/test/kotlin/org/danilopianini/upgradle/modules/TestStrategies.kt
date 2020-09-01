package org.danilopianini.upgradle.modules

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.io.File

abstract class ModuleWithStrategy(
    vararg strategies: String
) : AbstractModule(
    when (strategies.size) {
        0 -> emptyMap()
        1 -> makeMap(strategies[0])
        else -> makeMap(strategies.toList())
    }
) {
    override fun invoke(p1: File) = TODO("Not yet implemented")
    infix fun whenMatching(versions: List<String>) = versions.asSequence().filterByStrategy().toList()
    companion object {
        private fun makeMap(content: Any) = mapOf("strategy" to content)
    }
}

object All : ModuleWithStrategy("all")
object Latest : ModuleWithStrategy("latest")
object Next : ModuleWithStrategy("next")
object NextAndLatest : ModuleWithStrategy("latest", "next")

class TestStrategies : FreeSpec({
    val oldest = listOf("1")
    val newest = listOf("3")
    val versions = oldest + listOf("2") + newest
    "all should match all versions" {
        All whenMatching versions shouldBe versions
    }
    "latest should match last version" {
        Latest whenMatching versions shouldBe newest
    }
    "next should match the oldest version" {
        Next whenMatching versions shouldBe oldest
    }
    "hybrid should match newest and oldest" {
        NextAndLatest whenMatching versions shouldContainExactlyInAnyOrder oldest + newest
    }
})
