package proxy

import core.HeadersManipulator
import core.Name
import core.ResponseFilter
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.reactivex.core.MultiMap
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.core.http.HttpClient
import io.vertx.reactivex.core.http.HttpClientRequest
import io.vertx.reactivex.core.http.HttpClientResponse
import io.vertx.reactivex.core.http.HttpServerRequest
import io.vertx.reactivex.core.http.HttpServerResponse
import io.vertx.reactivex.ext.web.RoutingContext
import org.reflections.Reflections
import recorder.Record
import recorder.reactivex.RecorderService

//TODO refactor class to use RxJava
class ProxyHandler(private val config: JsonObject, private val recorderService: RecorderService) :
    Handler<RoutingContext> {

    private companion object {
        const val ROOT_PATH_KEY = "rootPath"
        const val ENDPOINT_PORT_KEY = "endpointPort"
        const val ENDPOINT_HOST_KEY = "endpointHost"
        const val CLIENT_OPTIONS_KEY = "clientOptions"
        const val EXTENSION_HEADERS_PACKAGE = "extension.header"
        val logger: Logger = LoggerFactory.getLogger(ProxyHandler::class.java)
    }

    private val allHeadersManipulators =
        Reflections(EXTENSION_HEADERS_PACKAGE).getSubTypesOf(HeadersManipulator::class.java)

    private val allResponseFilters =
        Reflections(EXTENSION_HEADERS_PACKAGE).getSubTypesOf(ResponseFilter::class.java)

    override fun handle(event: RoutingContext) {
        val httpClientOptions = config.getJsonObject(CLIENT_OPTIONS_KEY).mapTo(HttpClientOptions::class.java)
        val httpClient = event.vertx().createHttpClient(httpClientOptions)
        val request = event.request()
        val response = event.response()

        request.pause()
        recorderService.rxIsRecorderStarted().subscribe { recordStarted ->
            request.resume()
            if (recordStarted) {
                val endpointRequest =
                    getEndpointRequest(httpClient, request, response)
                endpointRequest.headers().setAll(request.headers())
                endpointRequest.isChunked = true
                request.handler { data ->
                    if (logger.isDebugEnabled) logger.debug("Request to endpoint data: $data")
                    endpointRequest.write(data)
                }
                request.endHandler {
                    if (logger.isDebugEnabled) logger.debug("Request to endpoint ended")
                    endpointRequest.end()
                }
            } else {
                if (logger.isDebugEnabled) logger.debug("Sending recorded response for ${request.uri()}")
                recorderService.rxGetRecordByPath(request.uri())
                    .subscribe(
                        { record ->
                            if (logger.isDebugEnabled) logger.debug("Record used: $record")
                            response.statusCode = record.statusCode
                            response.headers().setAll(MultiMap(record.headers))
                            response.isChunked = true
                            response.write(Buffer(record.data))
                            response.end()
                        },
                        { error ->
                            response.statusCode = HttpResponseStatus.NOT_FOUND.code()
                            response.end(error.message)
                        }
                    )
            }
        }

    }

    private fun getEndpointRequest(
        httpClient: HttpClient,
        request: HttpServerRequest,
        response: HttpServerResponse
    ): HttpClientRequest {
        val endpointPath = request.uri().substringAfter(config.getString(ROOT_PATH_KEY))
        val endpointPort = config.getInteger(ENDPOINT_PORT_KEY)
        val endpointHost = config.getString(ENDPOINT_HOST_KEY)
        val supportedHeaderManipulators = config.getJsonArray("headerManipulators").toSet()
        val supportedResponseFilters = config.getJsonArray("responseFilters").toSet()

        return httpClient.request(request.method(), endpointPort, endpointHost, endpointPath) { endpointResponse ->
            val record = Record()
            response.statusCode = endpointResponse.statusCode()
            response.headers().setAll(endpointResponse.headers())
            manipulateResponseHeaders(response.headers(), supportedHeaderManipulators)
            response.isChunked = true

            endpointResponse.handler { data ->
                if (logger.isDebugEnabled) logger.debug("Response from endpoint data: $data")
                response.write(data)
                record.appendBuffer(data.delegate)
            }
            endpointResponse.endHandler {
                if (logger.isDebugEnabled) logger.debug("Request from endpoint ended")
                record.statusCode = endpointResponse.statusCode()
                record.headers = endpointResponse.headers().delegate
                record.path = request.uri()
                if (shouldRecordResponse(endpointResponse, supportedResponseFilters)) {
                    recorderService.rxCreateRecord(record).subscribe()
                }
                response.end()
            }
        }
    }

    private fun manipulateResponseHeaders(headers: MultiMap, supportedHeadersManipulators: Set<Any>) {
        this.allHeadersManipulators.forEach {
            val name = it.getAnnotation(Name::class.java)
            if (name.value in supportedHeadersManipulators) {
                val headersManipulator = it.newInstance()
                headersManipulator.modify(headers.delegate)
            }
        }
    }

    private fun shouldRecordResponse(response: HttpClientResponse, supportedResponseFilters: Set<Any>): Boolean {
        var result = true
        this.allResponseFilters.forEach {
            val name = it.getAnnotation(Name::class.java)
            if (name.value in supportedResponseFilters) {
                val responseFilter = it.newInstance()
                result = result && responseFilter.test(response)
            }
        }
        return result
    }
}
