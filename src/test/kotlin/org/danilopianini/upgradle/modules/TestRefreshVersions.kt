package org.danilopianini.upgradle.modules

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

typealias Artifacts = List<String>
typealias OldVersions = List<String>
typealias NewVersions = List<String>
typealias MatchResult = Triple<Artifacts, OldVersions, NewVersions>

class TestRefreshVersions : FreeSpec({
    "refreshversions should extract correctly" - {
        "entries with a space" - {
            testEqualityOf(
                """
                 version.konf=6.0.0
                ### available=1.2.3.4
                """.trimIndent(),
                artifact = "konf",
                oldVersion = "6.0.0",
                newVersion = "1.2.3.4"
            )
        }

        "multiple entries" - {
            testEqualityOf(
                """
                plugin.com.dorongold.task-tree=1.5
                ##                 # available=1.2.3.4
                
                plugin.com.eden.orchidPlugin=version.orchid

                plugin.com.github.johnrengelman.shadow=6.0.0
                ##                         # available=1.2.3.4
                
                plugin.com.github.maiflai.scalatest=0.26
                ##                      # available=1.2.3.4                            
                """.trimIndent(),
                listOf("task-tree", "shadow", "scalatest"),
                listOf("1.5", "6.0.0", "0.26"),
                (0..2).map { "1.2.3.4" }
            )
        }

        "scala artifacts" - {
            testEqualityOf(
                """
                version.org.scalatest..scalatest_2.13=3.3.0-SNAP2
                ##                        # available=1.2.3.4
                """.trimIndent(),
                "scalatest_2.13",
                "3.3.0-SNAP2",
                "1.2.3.4"
            )
        }

        "shortened entries" - {
            testEqualityOf(
                """
                    version.protelis=13.3.9
                    ##   # available=1.2.3.4

                    version.scalacache=0.28.0
                    ##     # available=1.2.3.4
                """.trimIndent(),
                listOf("protelis", "scalacache"),
                listOf("13.3.9", "0.28.0"),
                (0..1).map { "1.2.3.4" }
            )
        }

        "multiple versions" - {
            extractFrom(
                """
                version.protelis=13.3.9
                ##   # available=1.2.3.4
                ##   # available=1.2.3.5
                ##   # available=1.2.3.6

                version.scalacache=0.28.0
                ##     # available=1.2.3.4
                ##     # available=1.2.3.5
                """.trimIndent()
            ).size shouldBe 5
        }
    }
}) {
    companion object {

        fun extractFrom(fileContent: String) = RefreshVersions.extractUpdatesRegex
            .findAll(fileContent)
            .flatMap { matchResult ->
                val (_, artifact, oldVersion, newVersions) = matchResult.destructured
                RefreshVersions.extractVersionsRegex.findAll(newVersions)
                    .map { listOf(artifact, oldVersion, it.destructured.component1()) }
            }
            .toList()

        fun testThat(
            fileContent: String,
            hasUpgradeCount: Int = 1,
            test: (Artifacts, OldVersions, NewVersions) -> Unit
        ) {
            val matches = extractFrom(fileContent)
            matches.size shouldBe hasUpgradeCount
            test(matches.map { it[0] }, matches.map { it[1] }, matches.map { it[2] })
        }

        fun testThatSingleMatch(fileContent: String, test: (String, String, String) -> Unit) {
            testThat(fileContent) { artifacts, oldVersions, newVersions ->
                test(artifacts.first(), oldVersions.first(), newVersions.first())
            }
        }

        fun testEqualityOf(
            fileContent: String,
            artifacts: Artifacts,
            oldVersions: OldVersions,
            newVersions: NewVersions
        ) {
            artifacts.size shouldBe oldVersions.size
            newVersions.size shouldBe oldVersions.size
            testThat(fileContent, hasUpgradeCount = artifacts.size) { arts, olds, news ->
                arts shouldBe artifacts
                olds shouldBe oldVersions
                news shouldBe newVersions
            }
        }

        fun testEqualityOf(fileContent: String, artifact: String, oldVersion: String, newVersion: String) {
            testEqualityOf(fileContent, listOf(artifact), listOf(oldVersion), listOf(newVersion))
        }
    }
}
