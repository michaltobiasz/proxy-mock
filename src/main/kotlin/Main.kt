import io.reactivex.Observable
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.config.ConfigRetriever
import io.vertx.reactivex.core.Vertx
import java.io.File

const val CONFIG_TYPE = "file"
const val CONFIG_FILE_FORMAT = "hocon"
const val CONFIG_PATH_KEY = "path"
const val MODULE_EXTENSIONS = "extensions"

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    createDirIfNecessary("extension/config")
    createDirIfNecessary("extension/db")

    val mainStore = createConfigStoreOptions("main.conf")
    val proxyStore = createConfigStoreOptions("extension/config/proxy.conf", optional = true)

    val configRetrieverOptions = ConfigRetrieverOptions()
        .addStore(mainStore)
        .addStore(proxyStore)

    val retriever = ConfigRetriever.create(vertx, configRetrieverOptions)

    retriever.rxGetConfig().subscribe { config -> onSuccess(config, vertx) }
}

private fun createConfigStoreOptions(path: String, optional: Boolean = false): ConfigStoreOptions {
    return ConfigStoreOptions()
        .setType(CONFIG_TYPE)
        .setFormat(CONFIG_FILE_FORMAT)
        .setConfig(JsonObject().put(CONFIG_PATH_KEY, path))
        .setOptional(optional)
}

private fun onSuccess(config: JsonObject, vertx: Vertx) {
    Observable.fromIterable(config.getJsonArray("modules"))
        .cast(String::class.java)
        .map { module -> module.split('=') }
        .map { parts -> Pair(parts[0], parts[1]) }
        .subscribe { (configName, moduleName) ->
            vertx.rxDeployVerticle(
                moduleName, getModuleDeploymentOptions(config, configName)
            ).subscribe()
        }
}

private fun getModuleDeploymentOptions(config: JsonObject, configName: String): DeploymentOptions {
    val allConfig = config.getJsonObject("config")
    val moduleConfig = allConfig.getJsonObject(configName)
    if (moduleConfig.containsKey(MODULE_EXTENSIONS)) {
        val extensions = moduleConfig.getJsonArray(MODULE_EXTENSIONS)
        for (extensionIndex in 0 until extensions.size()) {
            val extension = extensions.getString(extensionIndex)
            moduleConfig.put(extension, allConfig.getValue(extension))
        }
    }
    return DeploymentOptions().setConfig(moduleConfig)
}

private fun createDirIfNecessary(path: String) {
    val directory = File(path)
    if (directory.exists() && directory.isFile) {
        throw IllegalStateException("$directory already exists and is a file")
    } else if (!directory.exists()) {
        directory.mkdirs()
    }
}
