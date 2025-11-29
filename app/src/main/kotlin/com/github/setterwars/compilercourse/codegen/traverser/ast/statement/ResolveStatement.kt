package com.github.setterwars.compilercourse.codegen.traverser.ast.statement

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Call
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.If
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.traverser.ast.body.resolveBody
import com.github.setterwars.compilercourse.codegen.traverser.ast.expression.resolveExpression
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryArray
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryRecord
import com.github.setterwars.compilercourse.codegen.traverser.cell.load
import com.github.setterwars.compilercourse.codegen.traverser.cell.store
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.CodegenException
import com.github.setterwars.compilercourse.codegen.utils.name
import com.github.setterwars.compilercourse.parser.nodes.ArrayAccessor
import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.FieldAccessor
import com.github.setterwars.compilercourse.parser.nodes.IfStatement
import com.github.setterwars.compilercourse.parser.nodes.ModifiablePrimary
import com.github.setterwars.compilercourse.parser.nodes.RoutineCall

fun WasmContext.resolveAssignment(
    modifiablePrimary: ModifiablePrimary,
    expression: Expression,
): List<Instr> {
    val variable = declarationManager.resolveVariable(modifiablePrimary.variable.name())
    val er = resolveExpression(expression)
    return if (modifiablePrimary.accessors == null || modifiablePrimary.accessors.isEmpty()) {
        variable.store(er.instructions)
    } else {
        val iss = mutableListOf<Instr>()
        iss.addAll(er.instructions)
        iss.addAll(variable.load())
        var currentCellValueTypeOnStack = variable.cellValueType
        for ((index, accessor) in modifiablePrimary.accessors.withIndex()) {
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

                    if (index + 1 < modifiablePrimary.accessors.size) {
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

                    if (index + 1 < modifiablePrimary.accessors.size) {
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
        when (er.onStackValueType.toWasmValue()) {
            WasmValue.I32 -> I32Store()
            WasmValue.F64 -> F64Store()
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
    for (arg in routineCall.arguments) {
        addAll(resolveExpression(arg.expression).instructions)
    }
    val routineDescription = declarationManager.resolveRoutine(
        routineCall.routineName.name()
    )
    add(Call(routineDescription.index))
}