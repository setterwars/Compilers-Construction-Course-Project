package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.parser.nodes.*
import java.util.IdentityHashMap

class SemanticInfoStore {
    val infoByNode: MutableMap<Any, SemanticInfo> = IdentityHashMap()

    fun setSemanticInfo(node: Any, info: SemanticInfo) {
        infoByNode[node] = info
    }
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : SemanticInfo> get(node: Any): T = infoByNode[node] as T
}
