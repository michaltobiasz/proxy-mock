package core

import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router
import proxy.ProxyRouterConfigurator
import recorder.RecorderRouterConfigurator

interface RouterConfigurator {

    companion object {
        @JvmStatic
        fun create(extension: String): RouterConfigurator = Configurator.configuratorOf(extension).configurator
    }

    fun configure(vertx: Vertx, router: Router, config: JsonObject): Router

    private enum class Configurator(val configurator: RouterConfigurator) {
        PROXY(ProxyRouterConfigurator()),
        RECORDER(RecorderRouterConfigurator()),
        EMPTY(EmptyRouterConfigurator());

        companion object {
            fun configuratorOf(extension: String): Configurator {
                var result = EMPTY
                for (value in Configurator.values()) {
                    if (value.name.equals(extension, ignoreCase = true)) {
                        result = value
                    }
                }
                return result
            }
        }
    }
}