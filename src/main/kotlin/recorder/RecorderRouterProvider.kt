package recorder

import core.RouterProvider
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router
import recorder.reactivex.RecorderService

class RecorderRouterProvider : RouterProvider {

    private companion object {
        const val ADDRESS_KEY = "address"
    }

    override fun provideRouter(vertx: Vertx, config: JsonObject): Router {
        val router = Router.router(vertx)

        router.route("/*")
            .handler(
                RecorderHandler(
                    config,
                    RecorderService.createProxy(vertx, config.getString(ADDRESS_KEY))
                )
            )

        return router
    }

}
