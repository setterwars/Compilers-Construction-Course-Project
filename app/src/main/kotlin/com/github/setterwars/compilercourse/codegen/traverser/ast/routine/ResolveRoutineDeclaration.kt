package com.github.setterwars.compilercourse.codegen.traverser.ast.routine

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.FuncType
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Return
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmFunc
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.traverser.ast.body.resolveBody
import com.github.setterwars.compilercourse.codegen.traverser.ast.expression.resolveExpression
import com.github.setterwars.compilercourse.codegen.traverser.ast.type.resolveCellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.Routine
import com.github.setterwars.compilercourse.codegen.traverser.cell.adjustStackValue
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.name
import com.github.setterwars.compilercourse.parser.nodes.FullRoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.RoutineHeader
import com.github.setterwars.compilercourse.parser.nodes.SingleExpressionBody

fun WasmContext.resolveRoutineDeclaration(
    routineDeclaration: RoutineDeclaration
): WasmFunc? {
    resolveRoutineHeader(routineDeclaration.header)
    if (routineDeclaration.body == null) return null
    val routineName = routineDeclaration.header.name.name()
    declarationManager.enterRoutine(routineName)
    val body = resolveRoutineBody(routineDeclaration.body)
    declarationManager.exitRoutine()
    val routineDescription = declarationManager.resolveRoutine(routineDeclaration.header.name.name())
    return WasmFunc(
        type = FuncType(
            params = routineDescription.parameters.map { it.cellValueType.toWasmValue() },
            results = routineDescription.returnValueType?.let { listOf(it.toWasmValue()) } ?: emptyList(),
        ),
        locals = emptyList(),
        body = body,
        name = routineDescription.name,
        isStart = false,
    )
}

fun WasmContext.resolveRoutineHeader(
    routineHeader: RoutineHeader
) {
    val name = routineHeader.name.name()
    val parameters = routineHeader.parameters.parameters.map {
        Routine.Parameter(
            name = it.name.name(),
            cellValueType = resolveCellValueType(it.type)
        )
    }
    val returnType = routineHeader.returnType?.let { resolveCellValueType(it) }
    declarationManager.declareRoutine(name, returnType, parameters)
}

fun WasmContext.resolveRoutineBody(
    body: RoutineBody,
): List<Instr> {
    val routine = declarationManager.resolveRoutine(declarationManager.currentRoutine!!)
    return when (body) {
        is FullRoutineBody -> buildList {
            addAll(resolveBody(body.body))
            when (routine.returnValueType?.toWasmValue()) {
                WasmValue.I32 -> add(Block(WasmValue.I32, listOf(I32Const(0))))
                WasmValue.F64 -> add(Block(WasmValue.F64, listOf(F64Const(0.0))))
                null -> {}
            }
        }
        is SingleExpressionBody -> resolveSingleExpressionBody(body)
    }
}

fun WasmContext.resolveSingleExpressionBody(singleExpressionBody: SingleExpressionBody): List<Instr> = buildList {
    val routine = declarationManager.resolveRoutine(declarationManager.currentRoutine!!)
    singleExpressionBody.expression.let {
        val er = resolveExpression(it)
        addAll(er.instructions)
        adjustStackValue(routine.returnValueType!!, er.onStackValueType)
    }
}