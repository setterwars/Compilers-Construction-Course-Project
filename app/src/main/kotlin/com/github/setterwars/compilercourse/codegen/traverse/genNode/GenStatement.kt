package com.github.setterwars.compilercourse.codegen.traverse.genNode

import com.github.setterwars.compilercourse.codegen.ir.Call
import com.github.setterwars.compilercourse.codegen.ir.Block
import com.github.setterwars.compilercourse.codegen.ir.Br
import com.github.setterwars.compilercourse.codegen.ir.BrIf
import com.github.setterwars.compilercourse.codegen.ir.F64Store
import com.github.setterwars.compilercourse.codegen.ir.I32Const
import com.github.setterwars.compilercourse.codegen.ir.I32Store
import com.github.setterwars.compilercourse.codegen.ir.I32Unary
import com.github.setterwars.compilercourse.codegen.ir.I32UnaryOp
import com.github.setterwars.compilercourse.codegen.ir.If
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.ir.Loop
import com.github.setterwars.compilercourse.codegen.traverse.CellType
import com.github.setterwars.compilercourse.codegen.traverse.StackValue
import com.github.setterwars.compilercourse.codegen.traverse.WasmStructureGenerator
import com.github.setterwars.compilercourse.parser.nodes.Assignment
import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.IfStatement
import com.github.setterwars.compilercourse.parser.nodes.RoutineCall
import com.github.setterwars.compilercourse.parser.nodes.Statement
import com.github.setterwars.compilercourse.parser.nodes.WhileLoop
import com.github.setterwars.compilercourse.semantic.extensionNodes.RemovedStatement

// Produce instr that will execute certain statement
fun WasmStructureGenerator.genStatement(statement: Statement): List<Instr> {
    return when (statement) {
        is Assignment -> genAssignment(statement)
        is RoutineCall -> genRoutineCall(statement).instructions
        is WhileLoop -> genWhileLoop(statement)
        is IfStatement -> genIfStatement(statement)
        is RemovedStatement -> emptyList()
        else -> TODO()
    }
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

data class RoutineCallResult(
    val instructions: List<Instr>,
    val stackValue: StackValue?,
)

fun WasmStructureGenerator.genRoutineCall(routineCall: RoutineCall): RoutineCallResult {
    val result = mutableListOf<Instr>()
    val routineName = routineCall.routineName.token.lexeme
    val rd = declarationManager.getRoutine(routineName)
    for ((i, vd) in rd.parameters.withIndex()) {
        result.add(I32Const(vd.address))
        result.addAll(assignExpressionToAddressOnStack(vd.cellType, routineCall.arguments[i].expression))
    }
    result.add(Call(rd.orderIndex))
    return RoutineCallResult(
        instructions = result,
        stackValue = rd.returnValue
    )
}

fun WasmStructureGenerator.genIfStatement(ifStatement: IfStatement): List<Instr> {
    val conditionResult = genExpression(ifStatement.condition)

    val thenInstrs = genBody(ifStatement.thenBody)
    val elseInstrs = ifStatement.elseBody?.let { genBody(it) } ?: emptyList()

    return buildList {
        addAll(conditionResult.instructions)
        add(If(resultType = null, thenInstrs = thenInstrs, elseInstrs = elseInstrs))
    }
}

fun WasmStructureGenerator.genWhileLoop(whileLoop: WhileLoop): List<Instr> {
    val conditionResult = genExpression(whileLoop.condition)
    val bodyInstrs = genBody(whileLoop.body)

    val loopInstrs = buildList<Instr> {
        addAll(conditionResult.instructions)
        add(I32Unary(I32UnaryOp.EQZ))
        add(BrIf(1))
        addAll(bodyInstrs)
        add(Br(0))
    }

    val blockInstrs = listOf(
        Loop(
            resultType = null,
            instructions = loopInstrs,
        )
    )

    return listOf(
        Block(
            resultType = null,
            instructions = blockInstrs,
        )
    )
}

