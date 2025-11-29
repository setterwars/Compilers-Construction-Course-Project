package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class RoutinesManager {
    data class RoutineDescription(
        val name: String,
        val returnValueType: CellValueType?,
        val parameters: List<RoutineParameter>,
        val index: Int,
    ) {
        data class RoutineParameter(val name: String, val cellValueType: CellValueType)
    }


    private val routines = mutableMapOf<String, RoutineDescription>()

    // If the exact same routine was already declared - then skip it
    // Otherwise throw exception
    fun declareRoutine(
        name: String,
        returnValueType: CellValueType?,
        parameters: List<RoutineDescription.RoutineParameter>
    ) {
        if (name in routines.keys) {
            val r = routines[name]!!
            if (r.returnValueType != returnValueType && r.parameters != parameters) {
                throw CodegenException()
            }
        }
        routines[name] = RoutineDescription(
            name = name,
            returnValueType = returnValueType,
            parameters = parameters,
            index = routines.size,
        )
    }

    fun getRoutineOrNull(name: String): RoutineDescription? {
        return routines[name]
    }

    fun getRoutine(name: String): RoutineDescription {
        return routines[name]!!
    }
}