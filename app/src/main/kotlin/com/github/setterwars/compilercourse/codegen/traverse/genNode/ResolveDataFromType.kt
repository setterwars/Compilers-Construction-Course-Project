package com.github.setterwars.compilercourse.codegen.traverse.genNode

import com.github.setterwars.compilercourse.codegen.traverse.MemArray
import com.github.setterwars.compilercourse.codegen.traverse.CellType
import com.github.setterwars.compilercourse.codegen.traverse.CodegenException
import com.github.setterwars.compilercourse.codegen.traverse.MemRecord
import com.github.setterwars.compilercourse.codegen.traverse.WasmStructureGenerator
import com.github.setterwars.compilercourse.parser.nodes.ArrayType
import com.github.setterwars.compilercourse.parser.nodes.DeclaredType
import com.github.setterwars.compilercourse.parser.nodes.PrimitiveType
import com.github.setterwars.compilercourse.parser.nodes.RecordType
import com.github.setterwars.compilercourse.parser.nodes.Type
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationNoType
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationWithType
import com.github.setterwars.compilercourse.semantic.ExpressionSemanticInfo
import com.github.setterwars.compilercourse.semantic.IntValue

// Given a type, what cell the variable having this type should point to?
fun WasmStructureGenerator.resolveCellTypeFromType(type: Type): CellType {
    return when (type) {
        is PrimitiveType -> {
            when (type) {
                PrimitiveType.BOOLEAN, PrimitiveType.INTEGER -> CellType.I32
                PrimitiveType.REAL -> CellType.F64
            }
        }
        is DeclaredType -> {
            declarationManager.getType(type.identifier.token.lexeme).cellType
        }
        is ArrayType -> {
            val stored = semanticInfoStore.get<ExpressionSemanticInfo>(type.expressionInBrackets!!).const
            val count = if (stored is IntValue) {
                stored.value.toInt()
            } else {
                throw CodegenException()
            }

            return CellType.ArrayReference(
                memArray = MemArray(
                    count = count,
                    cellType = resolveCellTypeFromType(type.type)
                )
            )
        }
        is RecordType -> {
            val fields = mutableListOf<Pair<String, CellType>>()
            for (variableDeclaration in type.declarations) {
                if (variableDeclaration is VariableDeclarationNoType) {
                    CodegenException()
                }
                if (variableDeclaration is VariableDeclarationWithType) {
                    fields.add(variableDeclaration.identifier.token.lexeme to resolveCellTypeFromType(variableDeclaration.type))
                }
            }
            CellType.RecordReference(
                memRecord = MemRecord(
                    fields = fields
                )
            )
        }
    }
}

