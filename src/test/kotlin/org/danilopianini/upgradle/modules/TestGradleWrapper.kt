package org.danilopianini.upgradle.modules

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldNotBe
import org.danilopianini.upgradle.modules.GradleVersion
import org.danilopianini.upgradle.modules.GradleWrapper
import kotlin.time.ExperimentalTime

@ExperimentalTime
class TestGradleWrapper : StringSpec({
    "Gradle versions should be accessible" {
        GradleWrapper.gradleVersions shouldNotBe emptyList<GradleVersion>()
        GradleWrapper.gradleVersions shouldContain GradleVersion(6, 0)
    }
})
