package db

import io.vertx.codegen.annotations.Fluent
import io.vertx.codegen.annotations.GenIgnore
import io.vertx.codegen.annotations.ProxyGen
import io.vertx.codegen.annotations.VertxGen
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import recorder.Record
import io.vertx.reactivex.ext.jdbc.JDBCClient as JDBCClientReactive

@ProxyGen
@VertxGen
interface DbService {

    @Fluent
    fun insertRecord(record: Record, resultHandler: Handler<AsyncResult<Int>>): DbService

    @Fluent
    fun updateRecord(id: String, record: Record, resultHandler: Handler<AsyncResult<Int>>): DbService

    @Fluent
    fun insertRecords(records: List<Record>, resultHandler: Handler<AsyncResult<List<Int>>>): DbService

    @Fluent
    fun getNewestRecords(resultHandler: Handler<AsyncResult<List<Record>>>): DbService

    @Fluent
    fun getRecord(id: String, resultHandler: Handler<AsyncResult<Record>>): DbService

    @Fluent
    fun getAllRecords(resultHandler: Handler<AsyncResult<List<Record>>>): DbService

    @Fluent
    fun deleteRecord(id: String, resultHandler: Handler<AsyncResult<Void>>): DbService

    @GenIgnore
    companion object {
        @JvmStatic
        fun create(dbClient: JDBCClient, queries: JsonObject): DbService =
            DbServiceImpl(JDBCClientReactive(dbClient), queries)

        @JvmStatic
        fun createProxy(vertx: Vertx, address: String): DbService =
            DbServiceVertxEBProxy(vertx, address)
    }
}