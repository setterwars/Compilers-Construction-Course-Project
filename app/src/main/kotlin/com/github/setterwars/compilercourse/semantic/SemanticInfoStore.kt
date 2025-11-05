package com.github.setterwars.compilercourse.semantic

import java.util.IdentityHashMap

class SemanticInfoStore {
    val infoByNode: MutableMap<Any, SemanticInfo> = IdentityHashMap()

    fun setSemanticInfo(node: Any, info: SemanticInfo) {
        infoByNode[node] = info
    }
    inline fun <reified T : SemanticInfo> get(node: Any): T = infoByNode[node] as T

    inline fun <reified T : SemanticInfo> getOrNull(node: Any): T? = infoByNode[node] as? T
}
