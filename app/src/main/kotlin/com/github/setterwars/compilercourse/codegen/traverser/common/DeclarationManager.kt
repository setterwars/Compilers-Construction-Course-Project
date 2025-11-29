package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellType
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.Variable
import com.github.setterwars.compilercourse.codegen.traverser.common.RoutinesManager.RoutineDescription
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class DeclarationManager(
    private val memoryManager: MemoryManager,
) {
    private val globalEntitiesManager = GlobalEntitiesManager()
    private val scopes = mutableListOf<ScopedEntitiesManager>()
    private val routinesManager = RoutinesManager()

    var currentRoutine: String? = null
        private set

    fun inScope(): Boolean = currentRoutine != null

    fun enterScope() {
        scopes.add(ScopedEntitiesManager(memoryManager))
    }

    fun exitScope() {
        scopes.removeLast()
    }

    fun enterRoutine(name: String) {
        if (currentRoutine == null) {
            throw CodegenException()
        }
        currentRoutine = name
    }

    fun exitRoutine() {
        currentRoutine = null
    }

    fun declareGlobalVariable(
        name: String,
        cellValueType: CellValueType,
    ) {
        if (currentRoutine != null) {
            throw CodegenException()
        }
        globalEntitiesManager.declareGlobalVariable(name, cellValueType)
    }

    fun declareLocalVariable(name: String, cellValueType: CellValueType) {
        scopes.last().declareScopedVariable(name, cellValueType)
    }

    fun addInitializerForGlobalVariable(name: String, initializer: Block) {
        globalEntitiesManager.addInitializer(name, initializer)
    }

    fun resolveVariable(name: String): Variable {
        for (scope in scopes.reversed()) {
            scope.getScopedVariable(name)?.let { scopedVariable ->
                return Variable(
                    name = name,
                    cellType = CellType.MemoryCell(scopedVariable.memoryAddress),
                    cellValueType = scopedVariable.cellValueType
                )
            }
        }
        currentRoutine?.let { routinesManager.getRoutine(it) }?.let { routineDescription ->
            routineDescription
                .parameters
                .indexOfFirst { it.name == name }
                .takeIf { it != -1 }
                ?.let { index ->
                    val parameter = routineDescription.parameters[index]
                    return Variable(
                        name = name,
                        cellType = CellType.LocalsCell(index),
                        cellValueType = parameter.cellValueType,
                    )
                }
        }
        globalEntitiesManager.getGlobalVariable(name)?.let { globalVariable ->
            return Variable(
                name = name,
                cellType = CellType.GlobalsCell(globalVariable.index),
                cellValueType = globalVariable.cellValueType
            )
        }
        throw CodegenException()
    }

    fun declareRoutine(
        name: String,
        returnValueType: CellValueType?,
        parameters: List<RoutineDescription.RoutineParameter>
    ) {
        routinesManager.declareRoutine(name, returnValueType, parameters)
    }

    fun resolveRoutine(
        name: String
    ): RoutineDescription {
        return routinesManager.getRoutine(name)
    }
}