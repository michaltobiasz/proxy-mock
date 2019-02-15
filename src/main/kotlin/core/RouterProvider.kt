package core

import io.vertx.reactivex.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.ext.web.Router

interface RouterProvider {

    fun provideRouter(vertx: Vertx, config: JsonObject): Router
}