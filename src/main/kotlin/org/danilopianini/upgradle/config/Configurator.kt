package org.danilopianini.upgradle.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue

data class GitHubAccess(val token: String? = null, val user: String? = null, val password: String? = null) {
    init {
        if (token == null) {
            require(user != null) {
                "Either an accessToken or a userName must be provided."
            }
            require(password != null) {
                "If no accessToken is provided, then a password is mandatory"
            }
        } else {
            require(password == null) {
                "You must not provide both accessToken and password."
            }
        }
    }
}

data class RepoDescriptor(val user: List<String>, val repo: List<String>, val branch: List<String> = listOf(".*"))
data class Configuration(val include: List<RepoDescriptor>, val exclude: List<RepoDescriptor>, val modules: List<String>)
object Configurator {
    fun load(body: Config.() -> Config): Configuration = Config().body()
            .at("").toValue()
}



fun main() {
    val result = Configurator.load {
        from.yaml.resource("baseconfig2.yml")
    }
    println(result)
}