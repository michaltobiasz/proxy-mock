package recorder

import db.DbService
import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.serviceproxy.ServiceBinder

class RecorderVerticle : AbstractVerticle() {

    private companion object {
        const val DB_CONFIG_KEY = "db"
        const val ADDRESS_KEY = "address"
    }

    override fun start(startFuture: Future<Void>?) {
        val config = config()
        val dbConfig = config().getJsonObject(DB_CONFIG_KEY)
        val dbService = DbService.createProxy(vertx.delegate, dbConfig.getString(ADDRESS_KEY))
        val recorderService = RecorderService.create(dbService)

        ServiceBinder(vertx.delegate)
            .setAddress(config.getString(ADDRESS_KEY))
            .register(RecorderService::class.java, recorderService)
    }

}