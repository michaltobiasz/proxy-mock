package recorder

import db.DbService
import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.GenIgnore
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import db.reactivex.DbService as DbServiceReactive

@ProxyGen
@VertxGen
interface RecorderService {

    @Fluent
    fun stop(resultHandler: Handler<AsyncResult<Void>>): RecorderService

    @Fluent
    fun start(resultHandler: Handler<AsyncResult<Void>>): RecorderService

    @Fluent
    fun isRecorderStarted(resultHandler: Handler<AsyncResult<Boolean>>): RecorderService

    @Fluent
    fun createRecord(record: Record, resultHandler: Handler<AsyncResult<Int>>): RecorderService

    @Fluent
    fun updateRecord(id: String, record: Record, resultHandler: Handler<AsyncResult<Int>>): RecorderService

    @Fluent
    fun getRecordByPath(path: String, resultHandler: Handler<AsyncResult<Record>>): RecorderService

    @Fluent
    fun getRecordById(id: String, resultHandler: Handler<AsyncResult<Record>>): RecorderService

    @Fluent
    fun deleteRecord(id: String, resultHandler: Handler<AsyncResult<Void>>): RecorderService

    @Fluent
    fun getAllRecords(resultHandler: Handler<AsyncResult<List<Record>>>): RecorderService

    @GenIgnore
    companion object {
        @JvmStatic
        fun create(dbService: DbService): RecorderService =
            RecorderServiceImpl(DbServiceReactive(dbService))

        @JvmStatic
        fun createProxy(vertx: Vertx, address: String): RecorderService =
            RecorderServiceVertxEBProxy(vertx, address)
    }
}