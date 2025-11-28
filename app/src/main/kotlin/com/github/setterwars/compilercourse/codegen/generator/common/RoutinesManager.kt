package com.github.setterwars.compilercourse.codegen.generator.common

import com.github.setterwars.compilercourse.codegen.generator.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class RoutinesManager {
    data class RoutineDescription(
        val name: String,
        val returnValueType: CellValueType,
        val parameters: List<RoutineParameter>,
        val index: Int,
    ) {
        data class RoutineParameter(val name: String, val cellValueType: CellValueType)
    }

    private val routines = mutableMapOf<String, RoutineDescription>()

    fun declareRoutine(
        name: String,
        returnValueType: CellValueType,
        parameters: List<RoutineDescription.RoutineParameter>
    ) {
        if (name in routines.keys) {
            throw CodegenException()
        }
        routines[name] = RoutineDescription(
            name = name,
            returnValueType = returnValueType,
            parameters = parameters,
            index = routines.size,
        )
    }

    fun getRoutine(name: String): RoutineDescription? {
        return routines[name]
    }
}