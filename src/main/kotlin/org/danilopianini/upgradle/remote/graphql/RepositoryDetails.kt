package org.danilopianini.upgradle.remote.graphql

data class RepositoryDetails<T : Paginated>(val data: Data<T>) : Paginated {
    override val pageInfo: PageInfo
        get() = data.repository.info.pageInfo

    data class Data<T : Paginated>(val repository: Repository<T>) {
        data class Repository<T : Paginated>(val info: T)
    }
}

data class TopicData(val nodes: List<Node?>?, override val pageInfo: PageInfo) : Paginated {
    data class Node(val topic: Topic) {
        data class Topic(val name: String)
    }
}
data class BranchData(val nodes: List<Node?>?, override val pageInfo: PageInfo) : Paginated {
    data class Node(val name: String)
}
