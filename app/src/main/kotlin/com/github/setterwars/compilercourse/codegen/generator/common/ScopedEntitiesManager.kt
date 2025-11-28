package com.github.setterwars.compilercourse.codegen.generator.common

import com.github.setterwars.compilercourse.codegen.generator.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class ScopedEntitiesManager(private val memoryManager: MemoryManager) {
    val scopedVariables = mutableMapOf<String, ScopedVariable>()
    val scopedTypes = mutableMapOf<String, ScopedType>()

    fun declareScopedVariable(name: String, cellValueType: CellValueType) {
        if (name in scopedVariables.keys) {
            throw CodegenException()
        }
        scopedVariables[name] = ScopedVariable(
            name,
            memoryManager.currentAddressPointer,
            cellValueType
        )
        memoryManager.addCell(cellValueType)
    }

    fun getScopedVariable(name: String): ScopedVariable? {
        return scopedVariables[name]
    }

    fun declareScopedType(name: String, cellValueType: CellValueType) {
        if (name in scopedTypes.keys) {
            throw CodegenException()
        }
        scopedTypes[name] = ScopedType(name, cellValueType)
    }

    fun getScopedType(name: String): ScopedType? {
        return scopedTypes[name]
    }

    data class ScopedVariable(
        val name: String,
        val memoryAddress: Int,
        val cellValueType: CellValueType,
    )

    data class ScopedType(
        val name: String,
        val cellValueType: CellValueType,
    )
}