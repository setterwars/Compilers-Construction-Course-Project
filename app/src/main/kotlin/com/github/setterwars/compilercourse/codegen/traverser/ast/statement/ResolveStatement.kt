package com.github.setterwars.compilercourse.codegen.traverser.ast.statement

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Br
import com.github.setterwars.compilercourse.codegen.bytecode.ir.BrIf
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Call
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Compare
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32RelOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Unary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32UnaryOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.If
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Loop
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Return
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.traverser.ast.body.resolveBody
import com.github.setterwars.compilercourse.codegen.traverser.ast.expression.resolveExpression
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryArray
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryRecord
import com.github.setterwars.compilercourse.codegen.traverser.cell.adjustStackValue
import com.github.setterwars.compilercourse.codegen.traverser.cell.load
import com.github.setterwars.compilercourse.codegen.traverser.cell.store
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.GlobalVariablesManager
import com.github.setterwars.compilercourse.codegen.traverser.common.MemoryManager
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.CodegenException
import com.github.setterwars.compilercourse.codegen.utils.name
import com.github.setterwars.compilercourse.parser.nodes.ArrayAccessor
import com.github.setterwars.compilercourse.parser.nodes.Assignment
import com.github.setterwars.compilercourse.parser.nodes.FieldAccessor
import com.github.setterwars.compilercourse.parser.nodes.ForLoop
import com.github.setterwars.compilercourse.parser.nodes.IfStatement
import com.github.setterwars.compilercourse.parser.nodes.PrintStatement
import com.github.setterwars.compilercourse.parser.nodes.ReturnStatement
import com.github.setterwars.compilercourse.parser.nodes.RoutineCall
import com.github.setterwars.compilercourse.parser.nodes.Statement
import com.github.setterwars.compilercourse.parser.nodes.WhileLoop

fun WasmContext.resolveStatement(
    statement: Statement
): Block {
    val instructions = when (statement) {
        is Assignment -> resolveAssignment(statement)
        is RoutineCall -> resolveRoutineCall(statement)
        is WhileLoop -> resolveWhileLoop(statement)
        is ForLoop -> resolveForLoop(statement)
        is PrintStatement -> TODO()
        is ReturnStatement -> resolveReturnStatement(statement)
        is IfStatement -> resolveIfStatement(statement)
    }
    return Block(
        resultType = null,
        instructions = instructions,
    )
}

fun WasmContext.resolveAssignment(
    assignment: Assignment,
): List<Instr> {
    val variable = declarationManager.resolveVariable(assignment.modifiablePrimary.variable.name())
    val er = resolveExpression(assignment.expression)
    return if (assignment.modifiablePrimary.accessors.isEmpty()) {
        variable.store {
            buildList {
                addAll(er.instructions)
                addAll(adjustStackValue(variable.cellValueType, er.onStackValueType))
            }
        }
    } else {
        val iss = mutableListOf<Instr>()
        iss.addAll(variable.load())
        var currentCellValueTypeOnStack = variable.cellValueType
        for ((index, accessor) in assignment.modifiablePrimary.accessors.withIndex()) {
            val accessorInstrs = when (accessor) {
                is FieldAccessor -> buildList {
                    val recordRef =
                        (currentCellValueTypeOnStack as? CellValueType.MemoryReference)
                            ?.referencable as? InMemoryRecord
                            ?: throw CodegenException()

                    val fieldIndex = recordRef
                        .fields
                        .indexOfFirst { it.name == accessor.identifier.token.lexeme }
                        .takeIf { it != -1 }
                        ?: throw CodegenException()
                    val offsetBytes = recordRef.fields.take(fieldIndex).sumOf { it.cellValueType.toWasmValue().bytes }
                    add(I32Const(offsetBytes))
                    add(I32Binary(I32BinOp.Add))

                    if (index + 1 < assignment.modifiablePrimary.accessors.size) {
                        when (recordRef.fields[fieldIndex].cellValueType.toWasmValue()) {
                            WasmValue.I32 -> add(I32Load())
                            WasmValue.F64 -> add(F64Load())
                        }
                    }
                    currentCellValueTypeOnStack = recordRef.fields[fieldIndex].cellValueType
                }

                is ArrayAccessor -> buildList {
                    val er = resolveExpression(accessor.expression)
                    val arrRef =
                        (currentCellValueTypeOnStack as? CellValueType.MemoryReference)
                            ?.referencable as? InMemoryArray
                            ?: throw CodegenException()
                    addAll(er.instructions)
                    add(I32Const(1))
                    add(I32Binary(I32BinOp.Sub))
                    add(I32Const(arrRef.cellValueType.toWasmValue().bytes))
                    add(I32Binary(I32BinOp.Mul))
                    add(I32Binary(I32BinOp.Add))

                    if (index + 1 < assignment.modifiablePrimary.accessors.size) {
                        when (arrRef.cellValueType.toWasmValue()) {
                            WasmValue.I32 -> add(I32Load())
                            WasmValue.F64 -> add(F64Load())
                        }
                    }
                    currentCellValueTypeOnStack = arrRef.cellValueType
                }
            }
            iss.addAll(accessorInstrs)
        }

        iss.addAll(er.instructions)
        iss.addAll(adjustStackValue(currentCellValueTypeOnStack, er.onStackValueType))
        when (currentCellValueTypeOnStack.toWasmValue()) {
            WasmValue.I32 -> iss.add(I32Store())
            WasmValue.F64 -> iss.add(F64Store())
        }
        iss
    }
}

fun WasmContext.resolveIfStatement(ifStatement: IfStatement): List<Instr> {
    val er = resolveExpression(ifStatement.condition)
    return buildList {
        addAll(er.instructions)
        add(
            If(
                resultType = null,
                thenInstrs = resolveBody(ifStatement.thenBody),
                elseInstrs = ifStatement.elseBody?.let { resolveBody(it) } ?: emptyList()
            )
        )
    }
}

fun WasmContext.resolveRoutineCall(
    routineCall: RoutineCall
): List<Instr> = buildList {
    add(I32Const(MemoryManager.FRAME_L_ADDR))
    add(I32Const(MemoryManager.FRAME_L_ADDR))
    add(I32Load())
    add(I32Const(MemoryManager.FRAME_R_ADDR))
    add(I32Const(MemoryManager.FRAME_R_ADDR))
    add(I32Load())


    val routine = declarationManager.resolveRoutine(routineCall.routineName.name())
    for ((i, arg) in routineCall.arguments.withIndex()) {
        val er = resolveExpression(arg.expression)
        addAll(er.instructions)
        addAll(adjustStackValue(routine.parameters[i].cellValueType, er.onStackValueType))
    }

    add(I32Const(MemoryManager.FRAME_L_ADDR))
    add(I32Const(MemoryManager.FRAME_R_ADDR))
    add(I32Load())
    add(I32Store())

    add(Call(routine.index))

    // Frame pointer restoration. Move the return value from the function to the temporary storage
    val reservedGlobal = when (routine.returnValueType?.toWasmValue()) {
        WasmValue.I32 -> declarationManager.resolveVariable(GlobalVariablesManager.ReservedGlobals.I32.nameOfGlobal)
        WasmValue.F64 -> declarationManager.resolveVariable(GlobalVariablesManager.ReservedGlobals.F64.nameOfGlobal)
        null -> null
    }
    addAll(reservedGlobal?.store(emptyList()) ?: emptyList())
    add(I32Store())
    add(I32Store())
    addAll(reservedGlobal?.load() ?: emptyList())
}

fun WasmContext.resolveWhileLoop(
    whileLoop: WhileLoop
): List<Instr> = buildList {
    add(
        Loop(
            resultType = null,
            instructions = buildList {
                val er = resolveExpression(whileLoop.condition)
                addAll(er.instructions)
                add(I32Unary(I32UnaryOp.EQZ))
                add(BrIf(1))
                addAll(resolveBody(whileLoop.body))
                add(Br(0))
            }
        )
    )
}

fun WasmContext.resolveForLoop(
    forLoop: ForLoop
): List<Instr> = buildList {
    declarationManager.enterScope()
    declarationManager.declareLocalVariable(
        forLoop.loopVariable.name(),
        CellValueType.I32
    )
    val loopIndexHolder = declarationManager.resolveVariable(forLoop.loopVariable.name())
    addAll(MemoryManager.moveRFrameForCellValueType(CellValueType.I32))
    addAll(loopIndexHolder.store(resolveExpression(forLoop.range.begin).instructions))
    add(
        Loop(
            resultType = null,
            instructions = buildList {
                if (forLoop.range.end != null) {
                    addAll(loopIndexHolder.load())
                    addAll(resolveExpression(forLoop.range.end).instructions)
                    if (forLoop.reverse) {
                        add(I32Compare(I32RelOp.GeS))
                    } else {
                        add(I32Compare(I32RelOp.LeS))
                    }
                    add(I32Unary(I32UnaryOp.EQZ))
                    add(BrIf(1))
                }
                addAll(resolveBody(forLoop.body))
                addAll(
                    loopIndexHolder.store {
                        buildList {
                            addAll(loopIndexHolder.load())
                            add(I32Const(1))
                            if (forLoop.reverse) {
                                add(I32Binary(I32BinOp.Sub))
                            } else {
                                add(I32Binary(I32BinOp.Add))
                            }
                        }
                    }
                )
                add(Br(0))
            }
        )
    )
    declarationManager.exitScope()
}

fun WasmContext.resolveReturnStatement(
    returnStatement: ReturnStatement
): List<Instr> = buildList {
    val routine = declarationManager.resolveRoutine(declarationManager.currentRoutine!!)
    returnStatement.expression?.let {
        val er = resolveExpression(it)
        addAll(er.instructions)
        addAll(adjustStackValue(routine.returnValueType!!, er.onStackValueType))
    }
    add(Return)
}