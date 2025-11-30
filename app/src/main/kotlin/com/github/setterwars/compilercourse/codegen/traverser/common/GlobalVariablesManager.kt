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
        for (reservedGlobal in ReservedGlobals.entries) {
            globalVariables[reservedGlobal.nameOfGlobal] = Variable(
                name = reservedGlobal.nameOfGlobal,
                cellValueType = reservedGlobal.cellValueType,
                variableType = VariableType.Global(index = reservedGlobal.ordinal)
            )
        }
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

    enum class ReservedGlobals(val nameOfGlobal: String, val cellValueType: CellValueType) {
        I32("#reserved_i32", CellValueType.I32),
        F64("#reserved_f64", CellValueType.F64)
    }
}