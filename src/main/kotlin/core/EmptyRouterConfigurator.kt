package core

import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router

class EmptyRouterConfigurator : RouterConfigurator {
    override fun configure(vertx: Vertx, router: Router, config: JsonObject): Router {
        return router
    }
}