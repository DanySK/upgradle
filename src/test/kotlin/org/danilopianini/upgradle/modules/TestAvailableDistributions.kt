package org.danilopianini.upgradle.modules

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainInOrder
import kotlin.time.ExperimentalTime

@ExperimentalTime
class TestAvailableDistributions : FreeSpec(
    {
        "The available distributions must contain Ubuntu versions in order" {
            val dists = TravisDist.availableDistributions
            dists.shouldContainInOrder("trusty", "xenial", "bionic", "focal")
        }
    }
)
