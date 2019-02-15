package recorder

import db.reactivex.DbService
import io.vertx.core.AsyncResult
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Handler
import io.vertx.reactivex.CompletableHelper
import io.vertx.reactivex.SingleHelper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import io.vertx.reactivex.core.Vertx as ReactiveVertex

class RecorderServiceImpl(private val dbService: DbService) : RecorderService {

    private companion object {
        const val RECORD_NOT_FOUND_MSG = "Record was not found for path"
    }

    private val records = ConcurrentHashMap<String, Record>()

    private val shouldRecord = AtomicBoolean(true)

    override fun start(resultHandler: Handler<AsyncResult<Void>>): RecorderService {
        shouldRecord.set(true)
        resultHandler.handle(succeededFuture())
        return this
    }

    @Synchronized
    override fun stop(resultHandler: Handler<AsyncResult<Void>>): RecorderService {
        this.shouldRecord.set(false)
        refreshRecords()
        resultHandler.handle(succeededFuture())
        return this
    }

    override fun isRecorderStarted(resultHandler: Handler<AsyncResult<Boolean>>): RecorderService {
        resultHandler.handle(succeededFuture(this.shouldRecord.get()))
        return this
    }

    override fun createRecord(
        record: Record, resultHandler: Handler<AsyncResult<Int>>
    ): RecorderService {
        this.dbService.rxInsertRecord(record)
            .subscribe(SingleHelper.toObserver(resultHandler))
        this.records[record.path] = record
        return this
    }

    override fun updateRecord(id: String, record: Record, resultHandler: Handler<AsyncResult<Int>>): RecorderService {
        this.dbService.rxUpdateRecord(id, record)
            .subscribe(SingleHelper.toObserver(resultHandler))
        this.records[record.path] = record
        return this
    }

    override fun deleteRecord(id: String, resultHandler: Handler<AsyncResult<Void>>): RecorderService {
        dbService.rxDeleteRecord(id)
            .subscribe(CompletableHelper.toObserver(resultHandler))
        return this
    }

    override fun getRecordByPath(path: String, resultHandler: Handler<AsyncResult<Record>>): RecorderService {
        val record = records[path]
        if (record != null) {
            resultHandler.handle(succeededFuture(record))
        } else {
            resultHandler.handle(failedFuture("$RECORD_NOT_FOUND_MSG: $path"))
        }
        return this
    }

    override fun getRecordById(id: String, resultHandler: Handler<AsyncResult<Record>>): RecorderService {
        dbService.rxGetRecord(id)
            .subscribe(SingleHelper.toObserver(resultHandler))
        return this
    }

    override fun getAllRecords(resultHandler: Handler<AsyncResult<List<Record>>>): RecorderService {
        dbService.rxGetAllRecords()
            .subscribe(SingleHelper.toObserver(resultHandler))
        return this
    }

    private fun refreshRecords() {
        this.dbService.rxGetNewestRecords()
            .subscribe { recordsList ->
                records.clear()
                recordsList.forEach { record -> this.records[record.path] = record }
            }
    }

}