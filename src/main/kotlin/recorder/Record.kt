package recorder

import io.vertx.codegen.annotations.DataObject
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject

@DataObject
class Record() {

    private companion object {
        const val ID = "id"
        const val STATUS_CODE = "statusCode"
        const val PATH = "path"
        const val HEADERS = "headers"
        const val DATA = "data"
        const val TIMESTAMP = "timestamp"
    }

    var id: Int = 0
    var statusCode: Int = 0
    lateinit var path: String
    lateinit var headers: MultiMap
    var data: Buffer? = null
    var timestamp: Long = System.nanoTime()

    constructor(json: JsonObject) : this() {
        this.id = json.getInteger(ID, -1)
        this.statusCode = json.getInteger(STATUS_CODE, -1)
        this.path = json.getString(PATH, "")
        this.headers = headersFromJson(json.getJsonObject(HEADERS, JsonObject()))
        this.data = json.getJsonObject(DATA, JsonObject()).toBuffer()
        this.timestamp = json.getLong(TIMESTAMP, System.nanoTime())
    }

    fun toJson(): JsonObject {
        val json = JsonObject()
        json.put(ID, this.id)
        json.put(STATUS_CODE, this.statusCode)
        json.put(PATH, this.path)
        json.put(HEADERS, headersToJson(this.headers))
        json.put(DATA, this.data?.toJsonObject())
        json.put(TIMESTAMP, this.timestamp)
        return json
    }

    fun appendBuffer(buffer: Buffer) {
        if (this.data == null) {
            this.data = Buffer.buffer(buffer.bytes)
        } else {
            this.data?.appendBuffer(buffer)
        }
    }

    fun isEmpty(): Boolean {
        return id == -1
    }

    private fun headersFromJson(jsonHeaders: JsonObject): MultiMap {
        val headers = MultiMap.caseInsensitiveMultiMap()
        jsonHeaders.forEach {
            headers.add(it.key, it.value as String)
        }
        return headers
    }

    private fun headersToJson(headers: MultiMap): JsonObject {
        val headersJson = JsonObject()
        for (entry in headers.entries()) {
            headersJson.put(entry.key, entry.value as String)
        }
        return headersJson
    }
}