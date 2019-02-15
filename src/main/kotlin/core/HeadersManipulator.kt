package core

import io.vertx.core.MultiMap

interface HeadersManipulator {

    fun modify(headers: MultiMap)
}