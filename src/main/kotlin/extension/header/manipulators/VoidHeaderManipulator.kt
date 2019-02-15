package extension.header.manipulators

import core.HeadersManipulator
import core.Name
import io.vertx.core.MultiMap

@Name(value = "voidHeaderManipulator")
class VoidHeaderManipulator : HeadersManipulator {
    override fun modify(headers: MultiMap) {
        println(headers)
    }
}