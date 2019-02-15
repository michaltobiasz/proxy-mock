package recorder

import core.RouterConfigurator
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router

class RecorderRouterConfigurator : RouterConfigurator {

    private companion object {
        const val ROOT_PATH_KEY = "rootPath"
    }

    override fun configure(vertx: Vertx, router: Router, config: JsonObject): Router {
        return router.mountSubRouter(config.getString(ROOT_PATH_KEY), RecorderRouterProvider().provideRouter(vertx, config))
    }
}