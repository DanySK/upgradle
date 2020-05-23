package org.danilopianini.upgradle.remote.graphql

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface Paginated {
    val pageInfo: PageInfo
}

data class PageInfo(val endCursor: String?, val hasNextPage: Boolean)

inline fun <T : Paginated> paginate(crossinline request: suspend (cursor: String?) -> T): Flow<T> = flow {
    var current = request(null).also { emit(it) }
    while (current.pageInfo.hasNextPage) {
        current = request(current.pageInfo.endCursor)
            .also { emit(it) }
    }
}
