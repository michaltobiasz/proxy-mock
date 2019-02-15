package extension.response.filters

import core.Name
import core.ResponseFilter
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.reactivex.core.http.HttpClientResponse

@Name(value = "successfulStatusResponseFilter")
class SuccessfulStatusResponseFilter : ResponseFilter {
    override fun test(response: HttpClientResponse): Boolean {
        return response.statusCode() == HttpResponseStatus.OK.code()
    }
}