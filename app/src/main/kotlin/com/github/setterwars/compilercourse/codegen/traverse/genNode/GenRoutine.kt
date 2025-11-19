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
    val returnValue: StackValue? = routineDeclaration.header.returnType?.let {
        resolveDataFromType(it)
    }?.toStackValue()
    val routineName = routineDeclaration.header.name.token.lexeme

    if (declarationManager.getRoutineOrNull(routineName) == null) {
        declarationManager.declareRoutine(
            name = routineName,
            returnValue = returnValue
        )

    }
    // Determine function type
    val params: List<ValueType> = routineDeclaration.header.parameters.parameters.map {
        val resolvedData = resolveDataFromType(it.type).toStackValue()
        when (resolvedData) {
            is StackValue.I32, is StackValue.ObjReference -> ValueType.I32
            is StackValue.F64 -> ValueType.F64
        }
    }
    val result: ValueType? = returnValue?.let {
        when (it) {
            is StackValue.I32, is StackValue.ObjReference -> ValueType.I32
            is StackValue.F64 -> ValueType.F64
        }
    }
    val functionType = FuncType(
        params = params,
        results = result?.let { listOf(it) } ?: emptyList()
    )
    val body = genRoutineBody(routineDeclaration.body!!) // TODO: add support for forward declaration
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



