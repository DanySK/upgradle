package org.danilopianini.upgradle.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.toValue
import org.danilopianini.upgradle.remote.Branch
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

    fun matches(repository: Repository) =
        ownersRegex.any { it matches repository.owner } &&
                reposRegex.any { it matches repository.name }

    fun matches(branch: Branch) =
        branchesRegex.any { it matches branch.name }

    fun matches(topic: String) =
        topicsRegex.any { it matches topic }

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
        require(color?.length == 6 && color.toLongOrNull(16) != null) {
            "Invalid color hexadecimal $color. Value must be six chars long in the [0-f] range"
        }
        return super.setColor(color)
    }

    companion object {
        fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
        fun randomColor() = Random.Default.nextBytes(3).toHexString()
    }
}

data class CommitAuthor(val name: String = "UpGradle [Bot]", val email: String = "<>")

data class Configuration(
    val includes: Set<RepoDescriptor>,
    val excludes: Set<RepoDescriptor>?,
    val modules: List<String>,
    val labels: List<ColoredLabel> = emptyList(),
    val author: CommitAuthor = CommitAuthor()
)

object Configurator {
    fun load(body: Config.() -> Config): Configuration = Config().body()
            .at("").toValue()
}
