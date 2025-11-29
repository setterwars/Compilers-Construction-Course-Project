package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.Variable
import com.github.setterwars.compilercourse.codegen.traverser.cell.VariableType
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class ScopedEntitiesManager{
    val scopedVariables = mutableMapOf<String, Variable>()

    fun declareScopedVariable(name: String, cellValueType: CellValueType, frameOffset: Int) {
        if (name in scopedVariables.keys) {
            throw CodegenException()
        }
        scopedVariables[name] = Variable(
            name = name,
            cellValueType = cellValueType,
            variableType = VariableType.Framed(frameOffset)
        )
    }

    fun getScopedVariable(name: String): Variable? {
        return scopedVariables[name]
    }
}