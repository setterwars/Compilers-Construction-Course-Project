package com.github.setterwars.compilercourse.codegen.generator.common

import com.github.setterwars.compilercourse.codegen.generator.cell.CellType
import com.github.setterwars.compilercourse.codegen.generator.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class DeclarationManager {
    val memoryManager = MemoryManager()
    val globalEntitiesManager = GlobalEntitiesManager()
    val routinesManager = RoutinesManager()
    val traverser = Traverser(memoryManager)

    data class VariableDescription(
        val name: String,
        val cellType: CellType,
        val cellValueType: CellValueType,
    )

    data class TypeDescription(
        val name: String,
        val cellValueType: CellValueType,
    )

    fun resolveVariable(name: String): VariableDescription {
        for (scope in traverser.scopes.reversed()) {
            scope.getScopedVariable(name)?.let { scopedVariable ->
                return VariableDescription(
                    name = name,
                    cellType = CellType.MemoryCell(scopedVariable.memoryAddress),
                    cellValueType = scopedVariable.cellValueType
                )
            }
        }
        traverser.currentRoutine?.let { routinesManager.getRoutine(it) }?.let { routineDescription ->
            routineDescription
                .parameters
                .indexOfFirst { it.name == name }
                .takeIf { it != -1 }
                ?.let { index ->
                    val parameter = routineDescription.parameters[index]
                    return VariableDescription(
                        name = name,
                        cellType = CellType.LocalsCell(index),
                        cellValueType = parameter.cellValueType,
                    )
                }
        }
        globalEntitiesManager.getGlobalVariable(name)?.let { globalVariable ->
            return VariableDescription(
                name = name,
                cellType = CellType.GlobalsCell(globalVariable.index),
                cellValueType = globalVariable.cellValueType
            )
        }
        throw CodegenException()
    }

    fun resolveType(name: String): TypeDescription {
        for (scope in traverser.scopes.reversed()) {
            scope.getScopedType(name)?.let { scopedType ->
                return TypeDescription(
                    name = name,
                    cellValueType = scopedType.cellValueType
                )
            }
        }
        globalEntitiesManager.getGlobalType(name)?.let { globalType ->
            return TypeDescription(
                name = name,
                cellValueType = globalType.cellValueType
            )
        }
        throw CodegenException()
    }
}