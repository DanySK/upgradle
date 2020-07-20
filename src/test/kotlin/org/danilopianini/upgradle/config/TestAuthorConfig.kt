package org.danilopianini.upgradle.config

import com.uchuhimo.konf.source.yaml
import io.kotest.assertions.inspecting
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class TestAuthorConfig : FreeSpec({
    "A loaded configuration" - {
        fun configOf(s: String): Configuration = Configurator.load { from.yaml.string(s) }
        val baseConfig =
            """
                includes:
                  - owners: .*
                    repos: .*
                    branches:
                      - master
                modules:
                  - GradleWrapper
                  - RefreshVersions
            """.trimIndent()

        "should use given author" {
            val authorNode =
                """
                    author:
                      name: Test McTestface
                      email: test@example.com
                """.trimIndent()

            inspecting(configOf("$baseConfig\n$authorNode").author) {
                name shouldBe "Test McTestface"
                email shouldBe "test@example.com"
            }
        }
        "should use default if none given" {
            inspecting(configOf(baseConfig).author) {
                name shouldBe "UpGradle [Bot]"
                email shouldBe "<>"
            }
        }
    }
})
