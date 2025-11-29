package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.Routine
import com.github.setterwars.compilercourse.codegen.traverser.cell.Variable
import com.github.setterwars.compilercourse.codegen.traverser.cell.VariableType
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class DeclarationManager {
    private val globalVariablesManager = GlobalVariablesManager()
    private val scopes = mutableListOf<ScopedEntitiesManager>()
    private val routinesManager = RoutinesManager()

    /** ---- SCOPES BEGIN ---- */
    var currentRoutine: String? = null
        private set
    var currentRoutineFunctionPointer = 0

    fun inScope(): Boolean = currentRoutine != null

    fun enterScope() {
        scopes.add(ScopedEntitiesManager())
    }

    fun exitScope() {
        scopes.removeLast()
    }

    fun enterRoutine(name: String) {
        if (currentRoutine == null) {
            throw CodegenException()
        }
        currentRoutineFunctionPointer = 0
        currentRoutine = name
    }

    fun exitRoutine() {
        currentRoutine = null
    }
    /** ---- END OF SCOPES ---- */

    /** ---- DECLARATION BEGIN ---- */
    fun declareGlobalVariable(
        name: String,
        cellValueType: CellValueType,
    ) {
        if (currentRoutine != null) {
            throw CodegenException()
        }
        globalVariablesManager.declareGlobalVariable(name, cellValueType)
    }

    fun addInitializerForGlobalVariable(name: String, initializer: Block) {
        globalVariablesManager.addInitializer(name, initializer)
    }

    fun declareLocalVariable(name: String, cellValueType: CellValueType) {
        scopes.last().declareScopedVariable(name, cellValueType, currentRoutineFunctionPointer)
        currentRoutineFunctionPointer += cellValueType.toWasmValue().bytes
    }

    fun resolveVariable(name: String): Variable {
        for (scope in scopes.reversed()) {
            scope.getScopedVariable(name)?.let { scopedVariable -> return scopedVariable }
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
                        cellValueType = parameter.cellValueType,
                        variableType = VariableType.Local(index)
                    )
                }
        }
        globalVariablesManager.getGlobalVariableOrNull(name)?.let { return it }
        throw CodegenException()
    }

    fun declareRoutine(
        name: String,
        returnValueType: CellValueType?,
        parameters: List<Routine.Parameter>
    ) {
        routinesManager.declareRoutine(name, returnValueType, parameters)
    }

    fun resolveRoutine(
        name: String
    ): Routine {
        return routinesManager.getRoutine(name)
    }
    /** ---- DECLARATION END ---- */
}