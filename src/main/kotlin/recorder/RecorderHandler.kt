package recorder

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.web.RoutingContext
import recorder.reactivex.RecorderService
import kotlin.streams.toList

class RecorderHandler(private val config: JsonObject, private val recorderService: RecorderService) :
    Handler<RoutingContext> {

    private companion object {
        const val START_PATH = "startPath"
        const val STOP_PATH = "stopPath"
        const val RECORDS_PATH = "recordsPath"
        const val RECORDING_STARTED_MSG = "Recording Started"
        const val RECORDING_STOPPED_MSG = "Recording Stopped"
        const val MESSAGE_KEY = "message"
        const val CONTENT_TYPE_APPLICATION_JSON = "application/json"
    }

    override fun handle(event: RoutingContext) {
        val uri = event.request().uri()
        when {
            uri.endsWith(config.getString(START_PATH)) -> handleStartPath(event)
            uri.endsWith(config.getString(STOP_PATH)) -> handleStopPath(event)
            uri.contains(config.getString(RECORDS_PATH)) -> handleRecordsPath(event)
        }
    }

    private fun handleStartPath(event: RoutingContext) {
        recorderService.rxStart()
            .subscribe(
                {
                    event.response()
                        .setStatusCode(HttpResponseStatus.OK.code())
                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_APPLICATION_JSON)
                        .end(wrapIntoJsonString(RECORDING_STARTED_MSG))
                },
                { error ->
                    handleError(error, event)
                }
            )
    }

    private fun handleStopPath(event: RoutingContext) {
        recorderService.rxStop()
            .subscribe(
                {
                    event.response()
                        .setStatusCode(HttpResponseStatus.OK.code())
                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_APPLICATION_JSON)
                        .end(wrapIntoJsonString(RECORDING_STOPPED_MSG))
                },
                { error ->
                    handleError(error, event)
                }
            )
    }

    private fun handleRecordsPath(event: RoutingContext) {
        when {
            isAllRecordsEvent(event) -> handleGetAllRecords(event)
            event.request().method() == HttpMethod.GET -> handleGetRecord(event)
            event.request().method() == HttpMethod.POST -> handleInsertRecord(event)
            event.request().method() == HttpMethod.PUT -> handleUpdateRecord(event)
            event.request().method() == HttpMethod.DELETE -> handleDeleteRecord(event)
            else -> handleNotSupportedPath(event)
        }
    }

    private fun isAllRecordsEvent(event: RoutingContext) =
        event.request().method() == HttpMethod.GET && event.request().uri().endsWith(config.getString(RECORDS_PATH))


    private fun handleGetAllRecords(event: RoutingContext) {
        recorderService.rxGetAllRecords()
            .subscribe(
                { records ->
                    val recordsAsJson = records.stream()
                        .map { record -> record.toJson() }
                        .toList()
                    event.response()
                        .setStatusCode(HttpResponseStatus.OK.code())
                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_APPLICATION_JSON)
                        .end(Buffer(JsonArray(recordsAsJson).toBuffer()))
                },
                { error ->
                    handleError(error, event)
                }
            )
    }

    private fun handleInsertRecord(event: RoutingContext) {
        event.request().bodyHandler { buffer ->
            try {
                recorderService.rxCreateRecord(Record(buffer.toJsonObject()))
                    .subscribe(
                        {
                            event.response()
                                .setStatusCode(HttpResponseStatus.OK.code())
                                .end()
                        },
                        { error ->
                            handleError(error, event)
                        })
            } catch (error: DecodeException) {
                handleError(error, event)
            }
        }
    }

    private fun handleGetRecord(event: RoutingContext) {
        val recordId = event.request().uri().substringAfterLast("/")
        recorderService.rxGetRecordById(recordId)
            .subscribe(
                { record ->
                    if (record.isEmpty()) {
                        event.response()
                            .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                            .end()
                    } else {
                        event.response()
                            .setStatusCode(HttpResponseStatus.OK.code())
                            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), CONTENT_TYPE_APPLICATION_JSON)
                            .end(Buffer(record.toJson().toBuffer()))
                    }
                },
                { error ->
                    handleError(error, event)
                }
            )
    }

    private fun handleUpdateRecord(event: RoutingContext) {
        val recordId = event.request().uri().substringAfterLast("/")
        event.request().bodyHandler { buffer ->
            try {
                recorderService.rxUpdateRecord(recordId, Record(buffer.toJsonObject()))
                    .subscribe(
                        {
                            event.response()
                                .setStatusCode(HttpResponseStatus.OK.code())
                                .end()
                        },
                        { error ->
                            handleError(error, event)
                        })
            } catch (error: DecodeException) {
                handleError(error, event)
            }
        }
    }

    private fun handleDeleteRecord(event: RoutingContext) {
        val recordId = event.request().uri().substringAfterLast("/")
        recorderService.rxDeleteRecord(recordId)
            .subscribe(
                {
                    event.response()
                        .setStatusCode(HttpResponseStatus.OK.code())
                        .end()
                },
                { error ->
                    handleError(error, event)
                }
            )
    }

    private fun handleError(error: Throwable, event: RoutingContext) {
        event.response()
            .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
            .end(wrapIntoJsonString(error.message))
    }

    private fun handleNotSupportedPath(event: RoutingContext) {
        event.response()
            .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
            .end()
    }

    private fun wrapIntoJsonString(message: String?): String = JsonObject().put(MESSAGE_KEY, message).toString()

}