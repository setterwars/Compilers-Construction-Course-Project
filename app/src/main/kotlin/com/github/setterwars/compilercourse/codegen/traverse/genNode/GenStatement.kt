package com.github.setterwars.compilercourse.codegen.traverse.genNode

import com.github.setterwars.compilercourse.codegen.ir.Call
import com.github.setterwars.compilercourse.codegen.ir.F64Store
import com.github.setterwars.compilercourse.codegen.ir.I32Const
import com.github.setterwars.compilercourse.codegen.ir.I32Store
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverse.CellType
import com.github.setterwars.compilercourse.codegen.traverse.DeclarationManager
import com.github.setterwars.compilercourse.codegen.traverse.StackValue
import com.github.setterwars.compilercourse.codegen.traverse.WasmStructureGenerator
import com.github.setterwars.compilercourse.parser.nodes.Assignment
import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.RoutineCall
import com.github.setterwars.compilercourse.parser.nodes.Statement

// Produce instr that will execute certain statement
fun WasmStructureGenerator.genStatement(statement: Statement): Instr {
    TODO()
}

fun WasmStructureGenerator.assignExpressionToAddressOnStack(
    cellType: CellType,
    expression: Expression
): List<Instr> {
    val result = mutableListOf<Instr>()
    val genExpressionResult = genExpression(expression)
    result.addAll(genExpressionResult.instructions)

    val instr = when (cellType) {
        is CellType.I32 -> I32Store()
        is CellType.F64 -> F64Store()
        else -> I32Store() // otherwise, copy address of the object into the cell
    }
    result.add(instr)
    return result
}

fun WasmStructureGenerator.genAssignment(assignment: Assignment): List<Instr> {
    val result = mutableListOf<Instr>()
    val modifiablePrimaryResolveResult = resolveModifiablePrimary(assignment.modifiablePrimary)
    result.addAll(modifiablePrimaryResolveResult.instructions)
    result.addAll(
        assignExpressionToAddressOnStack(
            modifiablePrimaryResolveResult.cellType,
            assignment.expression
        )
    )
    return result
}

fun WasmStructureGenerator.genRoutineCall(routineCall: RoutineCall): List<Instr> {
    val result = mutableListOf<Instr>()
    val routineName = routineCall.routineName.token.lexeme
    val rd = declarationManager.getRoutine(routineName)
    for ((i, vd) in rd.parameters.withIndex()) {
        result.add(I32Const(vd.address))
        assignExpressionToAddressOnStack(vd.cellType, routineCall.arguments[i].expression)
    }
    result.add(Call(rd.orderIndex))
    return result
}

