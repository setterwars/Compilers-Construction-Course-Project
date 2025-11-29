package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.Routine
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class RoutinesManager {
    val routines = mutableMapOf<String, Routine>()

    // If the exact same routine was already declared - then skip it
    // Otherwise throw exception
    fun declareRoutine(
        name: String,
        returnValueType: CellValueType?,
        parameters: List<Routine.Parameter>
    ) {
        if (name in routines.keys) {
            val r = routines[name]!!
            if (r.returnValueType != returnValueType && r.parameters != parameters) {
                throw CodegenException()
            }

        }
        routines[name] = Routine(
            name = name,
            returnValueType = returnValueType,
            parameters = parameters,
            index = routines.size,
        )
    }

    fun getRoutineOrNull(name: String): Routine? {
        return routines[name]
    }

    fun getRoutine(name: String): Routine {
        return routines[name]!!
    }
}