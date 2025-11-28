package com.github.setterwars.compilercourse.codegen.generator.common

import com.github.setterwars.compilercourse.codegen.generator.cell.CellType
import com.github.setterwars.compilercourse.codegen.generator.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class GlobalEntitiesManager {

    val globalVariables = mutableMapOf<String, GlobalVariable>()
    val globalTypes = mutableMapOf<String, GlobalType>()

    fun declareGlobalVariable(name: String, cellValueType: CellValueType) {
        if (name in globalVariables.keys) {
            throw CodegenException()
        }
        globalVariables[name] = GlobalVariable(name, globalVariables.size, cellValueType)
    }

    fun getGlobalVariable(name: String): GlobalVariable? {
        return globalVariables[name]
    }

    fun declareGlobalType(name: String, cellValueType: CellValueType) {
        if (name in globalTypes) {
            throw CodegenException()
        }
        globalTypes[name] = GlobalType(name, cellValueType)
    }

    fun getGlobalType(name: String): GlobalType? {
        return globalTypes[name]
    }

    data class GlobalVariable(
        val name: String,
        val index: Int,
        val cellValueType: CellValueType,
    )

    data class GlobalType(
        val name: String,
        val cellValueType: CellValueType,
    )
}