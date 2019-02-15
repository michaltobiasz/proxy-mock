package core

import io.vertx.reactivex.core.http.HttpClientResponse

interface ResponseFilter {

    fun test(response: HttpClientResponse): Boolean
}