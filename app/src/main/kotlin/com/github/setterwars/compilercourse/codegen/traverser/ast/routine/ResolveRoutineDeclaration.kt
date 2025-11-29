package com.github.setterwars.compilercourse.codegen.traverser.ast.routine

import com.github.setterwars.compilercourse.codegen.bytecode.ir.FuncType
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmFunc
import com.github.setterwars.compilercourse.codegen.traverser.ast.type.resolveCellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.RoutinesManager
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.name
import com.github.setterwars.compilercourse.parser.nodes.Body
import com.github.setterwars.compilercourse.parser.nodes.RoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.RoutineHeader

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
        RoutinesManager.RoutineDescription.RoutineParameter(
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
    TODO()
}