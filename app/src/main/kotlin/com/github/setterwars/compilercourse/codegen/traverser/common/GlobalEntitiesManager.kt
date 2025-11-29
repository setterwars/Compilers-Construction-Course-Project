package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class GlobalEntitiesManager {

    val globalVariables = mutableMapOf<String, GlobalVariable>()
    val initializers = mutableMapOf<String, Block>()

    fun declareGlobalVariable(
        name: String,
        cellValueType: CellValueType,
    ) {
        if (name in globalVariables.keys) {
            throw CodegenException()
        }
        globalVariables[name] = GlobalVariable(name, globalVariables.size, cellValueType)
    }

    fun getGlobalVariable(name: String): GlobalVariable? {
        return globalVariables[name]
    }

    fun addInitializer(name: String, initializer: Block) {
        if (name in initializers.keys) {
            throw CodegenException()
        }
        initializers[name] = initializer
    }

    data class GlobalVariable(
        val name: String,
        val index: Int,
        val cellValueType: CellValueType,
    )
}