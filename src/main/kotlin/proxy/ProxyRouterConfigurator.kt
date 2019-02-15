package proxy

import core.RouterConfigurator
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router

class ProxyRouterConfigurator : RouterConfigurator {

    private companion object {
        const val ROOT_PATH_KEY = "rootPath"
        const val PROXIES_CONFIG_KEY = "proxies"
        const val RECORDER_ADDRESS_KEY = "recorderAddress"
    }

    override fun configure(vertx: Vertx, router: Router, config: JsonObject): Router {
        val proxies = config.getJsonArray(PROXIES_CONFIG_KEY, JsonArray())
        for (proxyIndex in 0 until proxies.size()) {
            val proxy = proxies.getJsonObject(proxyIndex)
            proxy.put(RECORDER_ADDRESS_KEY, config.getString(RECORDER_ADDRESS_KEY))
            router.mountSubRouter(proxy.getString(ROOT_PATH_KEY), ProxyRouterProvider().provideRouter(vertx, proxy))
        }
        return router
    }
}