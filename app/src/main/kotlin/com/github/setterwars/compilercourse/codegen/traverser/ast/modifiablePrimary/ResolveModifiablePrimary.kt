package com.github.setterwars.compilercourse.codegen.traverser.ast.modifiablePrimary

import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.traverser.ast.expression.resolveExpression
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryArray
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryRecord
import com.github.setterwars.compilercourse.codegen.traverser.cell.load
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.CodegenException
import com.github.setterwars.compilercourse.parser.nodes.ArrayAccessor
import com.github.setterwars.compilercourse.parser.nodes.FieldAccessor
import com.github.setterwars.compilercourse.parser.nodes.ModifiablePrimary

data class ResolveModifiablePrimaryResult(
    val instructions: List<Instr>,
    val cellValueType: CellValueType,
)

// Given modifiable primary, put on the stack the value this modifiablePrimary contains
fun WasmContext.resolveModifiablePrimary(
    modifiablePrimary: ModifiablePrimary,
): ResolveModifiablePrimaryResult {
    val variable = declarationManager.resolveVariable(modifiablePrimary.variable.token.lexeme)
    val iss = mutableListOf<Instr>()
    iss.addAll(variable.load())

    var currentCellValueTypeOnStack = variable.cellValueType
    for (accessor in modifiablePrimary.accessors ?: emptyList()) {
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
                when (recordRef.fields[fieldIndex].cellValueType.toWasmValue()) {
                    WasmValue.I32 -> add(I32Load())
                    WasmValue.F64 -> add(F64Load())
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
                when (arrRef.cellValueType.toWasmValue()) {
                    WasmValue.I32 -> add(I32Load())
                    WasmValue.F64 -> add(F64Load())
                }
                currentCellValueTypeOnStack = arrRef.cellValueType
            }
        }
        iss.addAll(accessorInstrs)
    }
    return ResolveModifiablePrimaryResult(
        instructions = iss,
        cellValueType = currentCellValueTypeOnStack,
    )
}