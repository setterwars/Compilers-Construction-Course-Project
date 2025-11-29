package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.Variable
import com.github.setterwars.compilercourse.codegen.traverser.cell.VariableType
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class GlobalVariablesManager {

    val globalVariables = mutableMapOf<String, Variable>()
    val initializers = mutableMapOf<String, Block>()

    init {
        declareGlobalVariable("#reserved_i32", CellValueType.I32)
        declareGlobalVariable("#reserved_f64", CellValueType.F64)
    }

    fun declareGlobalVariable(
        name: String,
        cellValueType: CellValueType,
    ) {
        if (name in globalVariables.keys) {
            throw CodegenException()
        }
        globalVariables[name] = Variable(name, cellValueType, VariableType.Global(globalVariables.size))
    }

    fun getGlobalVariableOrNull(name: String): Variable? {
        return globalVariables[name]
    }

    fun addInitializer(name: String, initializer: Block) {
        if (name in initializers.keys) {
            throw CodegenException()
        }
        initializers[name] = initializer
    }

    companion object {
        const val RESERVED_I32 = "#reserved_i32"
        const val RESERVED_F64 = "#reserved_f64"
    }
}