package db

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.jdbc.JDBCClient
import io.vertx.serviceproxy.ServiceBinder

class DbVerticle : AbstractVerticle() {

    private companion object {
        const val DB_URL_KEY = "dbUrl"
        const val DB_DRIVER_CLASS_KEY = "dbDriverClass"
        const val DB_URL_CONFIGURATION_KEY = "url"
        const val DB_DRIVER_CLASS_CONFIGURATION_KEY = "driver_class"
        const val ADDRESS_KEY = "address"
        const val QUERIES_KEY = "queries"
    }

    override fun start(startFuture: Future<Void>?) {
        val config = config()
        val dbClientConfig = JsonObject()
            .put(DB_URL_CONFIGURATION_KEY, config.getString(DB_URL_KEY))
            .put(DB_DRIVER_CLASS_CONFIGURATION_KEY, config.getString(DB_DRIVER_CLASS_KEY))
        val dbClient = JDBCClient.createShared(vertx, dbClientConfig)
        val dbService = DbService.create(dbClient.delegate, config.getJsonObject(QUERIES_KEY))

        ServiceBinder(vertx.delegate)
            .setAddress(config.getString(ADDRESS_KEY))
            .register(DbService::class.java, dbService)
    }

}