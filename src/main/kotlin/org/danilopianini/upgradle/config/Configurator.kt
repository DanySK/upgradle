package org.danilopianini.upgradle.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.toValue
import org.danilopianini.upgradle.remote.BranchSource
import org.danilopianini.upgradle.remote.Repository
import org.eclipse.egit.github.core.Label
import kotlin.random.Random

data class RepoDescriptor(
    private val owners: List<String>,
    private val repos: List<String>,
    private val branches: List<String> = listOf(".*"),
    private val topics: List<String> = listOf(".*")
) {
    private val ownersRegex by lazy { owners.toRegex() }
    private val reposRegex by lazy { repos.toRegex() }
    private val branchesRegex by lazy { branches.toRegex() }
    private val topicsRegex by lazy { topics.toRegex() }

    fun matches(remoteBranch: BranchSource.SelectedRemoteBranch) =
        ownersRegex.any { it matches remoteBranch.repository.owner } &&
            reposRegex.any { it matches remoteBranch.repository.name } &&
            branchesRegex.any { it matches remoteBranch.branch.name } &&
            matchesTopics(remoteBranch.repository)

    private fun matchesTopics(repository: Repository) = repository.topics.isEmpty() ||
        topicsRegex.any { topicRegex -> repository.topics.any { topicRegex matches it } }

    companion object {
        private fun List<String>.toRegex() = map { Regex(it) }
    }
}

class ColoredLabel : Label() {

    override fun getColor(): String {
        if (super.getColor() == null) {
            setColor(randomColor())
        }
        return super.getColor()
    }

    override fun setColor(color: String?): Label {
        require(color?.length == COLOR_HEX_LENGTH && color.toLongOrNull(16) != null) {
            "Invalid color hexadecimal $color. Value must be six chars long in the [0-f] range"
        }
        return super.setColor(color)
    }

    companion object {
        const val COLOR_HEX_LENGTH = 3 * 2
        fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
        fun randomColor() = Random.Default.nextBytes(3).toHexString()
    }
}

data class CommitAuthor(val name: String = "UpGradle [Bot]", val email: String = "<>")

typealias ModuleOptions = Map<String, String>

data class UpgradleModule(val name: String, val options: ModuleOptions = emptyMap())

data class Configuration(
    val includes: Set<RepoDescriptor>,
    val excludes: Set<RepoDescriptor>?,
    val modules: List<UpgradleModule>,
    val labels: List<ColoredLabel> = emptyList(),
    val author: CommitAuthor = CommitAuthor()
)

object Configurator {
    fun load(body: Config.() -> Config): Configuration = Config().body().at("").toValue()
}
