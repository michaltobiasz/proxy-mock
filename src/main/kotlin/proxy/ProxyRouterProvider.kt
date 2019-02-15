package proxy

import core.RouterProvider
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router
import recorder.reactivex.RecorderService

class ProxyRouterProvider : RouterProvider {

    private companion object {
        const val RECORDER_ADDRESS_KEY = "recorderAddress"
    }

    override fun provideRouter(vertx: Vertx, config: JsonObject): Router {
        val router = Router.router(vertx)
        router.route("/*")
            .handler(ProxyHandler(config, RecorderService.createProxy(vertx, config.getString(RECORDER_ADDRESS_KEY))))

        return router
    }

}
