package server

import core.RouterConfigurator
import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Router

class ServerVerticle : AbstractVerticle() {

    private companion object {
        const val SERVER_PORT_KEY = "port"
        const val SERVER_HOST_KEY = "host"
        const val SERVER_EXTENSIONS_KEY = "extensions"
    }

    override fun start(startFuture: Future<Void>) {
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        val config = config()
        val extensions = config.getJsonArray(SERVER_EXTENSIONS_KEY)

        for (extensionIndex in 0 until extensions.size()) {
            val extension = extensions.getString(extensionIndex)
            RouterConfigurator.create(extension).configure(vertx, router, config.getJsonObject(extension))
        }

        server
            .requestHandler { router.accept(it) }
            .rxListen(config.getInteger(SERVER_PORT_KEY), config.getString(SERVER_HOST_KEY))
            .subscribe()
    }
}
