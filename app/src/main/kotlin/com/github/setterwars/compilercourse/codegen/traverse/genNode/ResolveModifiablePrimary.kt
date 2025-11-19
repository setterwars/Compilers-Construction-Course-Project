package com.github.setterwars.compilercourse.codegen.traverse.genNode

import com.github.setterwars.compilercourse.codegen.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.ir.I32Const
import com.github.setterwars.compilercourse.codegen.ir.I32Load
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverse.CellType
import com.github.setterwars.compilercourse.codegen.traverse.CodegenException
import com.github.setterwars.compilercourse.codegen.traverse.WasmStructureGenerator
import com.github.setterwars.compilercourse.parser.nodes.ArrayAccessor
import com.github.setterwars.compilercourse.parser.nodes.FieldAccessor
import com.github.setterwars.compilercourse.parser.nodes.ModifiablePrimary

data class ModifiablePrimaryResolveResult(
    val cellType: CellType,
    val instructions: List<Instr>
)


// Will create instructions that will put on the stack the address/index of the resolved cell
// Also, will return the type of that cell
fun WasmStructureGenerator.resolveModifiablePrimary(modifiablePrimary: ModifiablePrimary): ModifiablePrimaryResolveResult {
    val result = mutableListOf<Instr>()
    val variableDescription = declarationManager.getVariable(modifiablePrimary.variable.token.lexeme)
    var currentCellType = variableDescription.cellType

    result.add(I32Const(variableDescription.address))
    if (modifiablePrimary.accessors != null) {
        for (accessor in modifiablePrimary.accessors) {
            when (accessor) {
                is ArrayAccessor -> {
                    if (currentCellType is CellType.ArrayReference) {
                        result.add(I32Load())
                        result.addAll(genExpression(accessor.expression).instructions)
                        result.add(I32Const(1))
                        result.add(I32Binary(I32BinOp.Sub))
                        result.add(I32Const(currentCellType.memArray.cellType.bytesSize))
                        result.add(I32Binary(I32BinOp.Mul))

                        currentCellType = currentCellType.memArray.cellType
                    } else {
                        throw CodegenException()
                    }
                }

                is FieldAccessor -> {
                    if (currentCellType is CellType.RecordReference) {
                        val neededFieldIndex = currentCellType
                            .memRecord
                            .fields
                            .indexOfFirst { it.first == accessor.identifier.token.lexeme }
                        result.add(I32Load())
                        for (i in 0..<neededFieldIndex) {
                            result.add(I32Const(currentCellType.memRecord.fields[i].second.bytesSize))
                            result.add(I32Binary(I32BinOp.Add))
                        }

                        currentCellType = currentCellType.memRecord.fields[neededFieldIndex].second
                    } else {
                        throw CodegenException()
                    }
                }
            }
        }
    }

    return ModifiablePrimaryResolveResult(
        cellType = currentCellType,
        instructions = result,
    )
}