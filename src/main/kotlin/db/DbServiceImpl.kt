package db

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.CompletableHelper
import io.vertx.reactivex.SingleHelper
import io.vertx.reactivex.ext.jdbc.JDBCClient
import recorder.Record
import java.util.stream.Collectors
import kotlin.streams.toList

class DbServiceImpl(private val dbClient: JDBCClient, private val queries: JsonObject) : DbService {

    private companion object {
        const val Q_SET_MYSQL_DATABASE_DIALECT = "setMySqlDatabaseDialect"
        const val Q_CREATE_RECORDS_TABLE = "createRecordsTable"
        const val Q_INSERT_RECORD = "insertRecord"
        const val Q_GET_NEWEST_RECORDS = "getNewestRecords"
        const val Q_GET_RECORD = "getRecord"
        const val Q_DELETE_RECORD = "deleteRecord"
        const val Q_GET_ALL_RECORDS = "getAllRecords"
        const val Q_UPDATE_RECORD = "updateRecord"
        const val T_RECORD_COLUMN = "RECORD"
        const val T_ID_COLUMN = "ID"
    }

    init {
        this.dbClient.rxCall(this.queries.getString(Q_SET_MYSQL_DATABASE_DIALECT)).subscribe()
        this.dbClient.rxCall(this.queries.getString(Q_CREATE_RECORDS_TABLE)).subscribe()
    }

    override fun insertRecord(record: Record, resultHandler: Handler<AsyncResult<Int>>): DbService {
        this.dbClient.rxUpdateWithParams(this.queries.getString(Q_INSERT_RECORD), recordToJsonArray(record))
            .map { updateResult -> updateResult.updated }
            .subscribe(SingleHelper.toObserver(resultHandler))
        return this
    }

    override fun updateRecord(id: String, record: Record, resultHandler: Handler<AsyncResult<Int>>): DbService {
        val params = recordToJsonArray(record).add(id)

        this.dbClient.rxUpdateWithParams(this.queries.getString(Q_UPDATE_RECORD), params)
            .map { updateResult -> updateResult.updated }
            .subscribe(SingleHelper.toObserver(resultHandler))
        return this
    }

    override fun insertRecords(records: List<Record>, resultHandler: Handler<AsyncResult<List<Int>>>): DbService {
        val params = records.stream()
            .map(this@DbServiceImpl::recordToJsonArray)
            .toList()

        this.dbClient.rxGetConnection()
            .flatMap { con -> con.rxBatchWithParams(this.queries.getString(Q_INSERT_RECORD), params) }
            .subscribe(SingleHelper.toObserver(resultHandler))
        return this
    }

    override fun getNewestRecords(resultHandler: Handler<AsyncResult<List<Record>>>): DbService {
        this.dbClient.rxQuery(this.queries.getString(Q_GET_NEWEST_RECORDS))
            .map { resultSet -> resultSet.rows }
            .map { list ->
                list.stream()
                    .map { json -> json.getString(T_RECORD_COLUMN) }
                    .map(::JsonObject)
                    .map(::Record)
                    .collect(Collectors.toList())
            }
            .subscribe(SingleHelper.toObserver(resultHandler))
        return this
    }

    override fun getAllRecords(resultHandler: Handler<AsyncResult<List<Record>>>): DbService {
        this.dbClient.rxQuery(this.queries.getString(Q_GET_ALL_RECORDS))
            .map { resultSet -> resultSet.rows }
            .map { list ->
                list.stream()
                    .map(this@DbServiceImpl::recordMapper)
                    .collect(Collectors.toList())
            }
            .subscribe(SingleHelper.toObserver(resultHandler))
        return this
    }

    override fun getRecord(id: String, resultHandler: Handler<AsyncResult<Record>>): DbService {
        val params = JsonArray().add(id)

        this.dbClient.rxQueryWithParams(this.queries.getString(Q_GET_RECORD), params)
            .map { resultSet -> resultSet.rows.getOrElse(0) { JsonObject().put(T_RECORD_COLUMN, "{}") } }
            .map(this@DbServiceImpl::recordMapper)
            .subscribe(SingleHelper.toObserver(resultHandler))
        return this
    }

    override fun deleteRecord(id: String, resultHandler: Handler<AsyncResult<Void>>): DbService {
        val params = JsonArray().add(id)

        this.dbClient.rxUpdateWithParams(this.queries.getString(Q_DELETE_RECORD), params)
            .ignoreElement()
            .subscribe(CompletableHelper.toObserver(resultHandler))
        return this
    }

    private fun recordMapper(row: JsonObject): Record {
        val recordJson = JsonObject(row.getString(T_RECORD_COLUMN, ""))
        val recordId = row.getInteger(T_ID_COLUMN, -1)
        val record = Record(recordJson)
        record.id = recordId
        return record
    }

    private fun recordToJsonArray(record: Record): JsonArray {
        return JsonArray()
            .add(record.path)
            .add(record.timestamp)
            .add(record.toJson().encode())
    }
}