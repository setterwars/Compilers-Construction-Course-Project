package com.github.setterwars.compilercourse.codegen.traverse.genNode

import com.github.setterwars.compilercourse.codegen.ir.FuncType
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.ir.ValueType
import com.github.setterwars.compilercourse.codegen.ir.WasmFunc
import com.github.setterwars.compilercourse.codegen.traverse.StackValue
import com.github.setterwars.compilercourse.codegen.traverse.WasmStructureGenerator
import com.github.setterwars.compilercourse.codegen.traverse.toStackValue
import com.github.setterwars.compilercourse.parser.nodes.FullRoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.SingleExpressionBody

fun WasmStructureGenerator.genRoutineDeclaration(routineDeclaration: RoutineDeclaration): WasmFunc {
    declarationManager.newScope()
    val returnValue: StackValue? = routineDeclaration.header.returnType?.let {
        resolveCellTypeFromType(it)
    }?.toStackValue()
    val routineName = routineDeclaration.header.name.token.lexeme

    val params = routineDeclaration.header.parameters.parameters.map {
        it.name.token.lexeme to resolveCellTypeFromType(it.type)
    }
    if (declarationManager.getRoutineOrNull(routineName) == null) {
        declarationManager.declareRoutine(
            name = routineName,
            returnValue = returnValue,
            parameters = params,
        )

    }
    // Determine function type
    val wasmParams: List<ValueType> = routineDeclaration.header.parameters.parameters.map {
        val resolvedData = resolveCellTypeFromType(it.type).toStackValue()
        when (resolvedData) {
            is StackValue.I32, is StackValue.CellAddress -> ValueType.I32
            is StackValue.F64 -> ValueType.F64
        }
    }
    val result: ValueType? = returnValue?.let {
        when (it) {
            is StackValue.I32, is StackValue.CellAddress -> ValueType.I32
            is StackValue.F64 -> ValueType.F64
        }
    }
    val functionType = FuncType(
        params = wasmParams,
        results = result?.let { listOf(it) } ?: emptyList()
    )
    declarationManager.declareFunctionVariables(routineName)
    val body = genRoutineBody(routineDeclaration.body!!) // TODO: add support for forward declaration
    declarationManager.exitScope()
    return WasmFunc(
        type = functionType,
        locals = emptyList(),
        body = body,
        name = routineName,
        isStart = false,
    )
}

fun WasmStructureGenerator.genRoutineBody(routineBody: RoutineBody): List<Instr> {
    return when (routineBody) {
        is FullRoutineBody -> genBody(routineBody.body)
        is SingleExpressionBody -> genExpression(routineBody.expression).instructions
    }
}



