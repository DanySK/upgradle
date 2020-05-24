package org.danilopianini.upgradle.remote.graphql

typealias RemoteRepository = UserRepositories.Data.Viewer.RepositoryConnection.Repository

data class UserRepositories(val data: Data) : Paginated {
    override val pageInfo: PageInfo
        get() = data.viewer.repositories.pageInfo

    data class Data(val viewer: Viewer) {
        data class Viewer(val repositories: RepositoryConnection) {
            data class RepositoryConnection(
                val nodes: List<Repository?>?,
                val pageInfo: PageInfo
            ) {
                data class Repository(
                    val isArchived: Boolean,
                    val isDisabled: Boolean,
                    val isMirror: Boolean,
                    val name: String,
                    val owner: Owner,
                    val url: String,
                    val viewerPermission: String?
                ) {
                    data class Owner(val login: String)
                }
            }
        }
    }
}
