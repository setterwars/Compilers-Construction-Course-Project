package com.github.setterwars.compilercourse.codegen.traverse.genNode

import com.github.setterwars.compilercourse.codegen.traverse.CodegenData
import com.github.setterwars.compilercourse.codegen.traverse.CodegenException
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

fun WasmStructureGenerator.resolveDataFromType(type: Type): CodegenData {
    return when (type) {
        is PrimitiveType -> {
            when (type) {
                PrimitiveType.BOOLEAN, PrimitiveType.INTEGER -> CodegenData.I32
                PrimitiveType.REAL -> CodegenData.F64
            }
        }
        is DeclaredType -> {
            declarationManager.getType(type.identifier.token.lexeme).data
        }
        is ArrayType -> {
            val stored = semanticInfoStore.get<ExpressionSemanticInfo>(type.expressionInBrackets!!).const
            val count = if (stored is IntValue) {
                stored.value.toInt()
            } else {
                throw CodegenException()
            }

            return CodegenData.Array(
                count = count,
                elementsData = resolveDataFromType(type.type)
            )
        }
        is RecordType -> {
            // TODO: Add support for variable declaration with initial value
            val fields = mutableListOf<Pair<String, CodegenData>>()
            for (variableDeclaration in type.declarations) {
                if (variableDeclaration is VariableDeclarationNoType) {
                    CodegenException()
                }
                if (variableDeclaration is VariableDeclarationWithType) {
                    fields.add(variableDeclaration.identifier.token.lexeme to resolveDataFromType(variableDeclaration.type))
                }
            }
            CodegenData.Record(fields = fields)
        }
    }
}