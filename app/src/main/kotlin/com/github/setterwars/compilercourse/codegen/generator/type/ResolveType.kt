package com.github.setterwars.compilercourse.codegen.generator.type

import com.github.setterwars.compilercourse.codegen.generator.cell.InMemoryArray
import com.github.setterwars.compilercourse.codegen.generator.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.generator.cell.InMemoryRecord
import com.github.setterwars.compilercourse.codegen.generator.common.DeclarationManager
import com.github.setterwars.compilercourse.codegen.utils.CodegenException
import com.github.setterwars.compilercourse.parser.nodes.ArrayType
import com.github.setterwars.compilercourse.parser.nodes.DeclaredType
import com.github.setterwars.compilercourse.parser.nodes.PrimitiveType
import com.github.setterwars.compilercourse.parser.nodes.RecordType
import com.github.setterwars.compilercourse.parser.nodes.Type
import com.github.setterwars.compilercourse.parser.nodes.UserType

fun resolveCellValueTypeFromAstType(
    type: Type,
    declarationManager: DeclarationManager,
): CellValueType {
    return when (type) {
        is PrimitiveType -> resolvePrimitiveType(type)
        is UserType -> TODO()
        is DeclaredType -> resolveDeclaredType(type, declarationManager)
    }
}

private fun resolvePrimitiveType(
    primitiveType: PrimitiveType
): CellValueType {
    return when (primitiveType) {
        PrimitiveType.INTEGER -> CellValueType.I32
        PrimitiveType.REAL -> CellValueType.F64
        PrimitiveType.BOOLEAN -> CellValueType.I32Boolean
    }
}

private fun resolveDeclaredType(
    declaredType: DeclaredType,
    declarationManager: DeclarationManager,
): CellValueType {
    return declarationManager.resolveType(declaredType.identifier.token.lexeme).cellValueType
}

private fun resolveUserType(
    userType: UserType,
    declarationManager: DeclarationManager
): CellValueType {
    return when (userType) {
        is ArrayType -> {
            if (userType.expressionInBrackets!!.additionalData is CompileTime)
            if (compileTimeValue !is CompileTimeInteger) {
                throw CodegenException()
            }
            return CellValueType.MemoryReference(
                InMemoryArray(
                    size = compileTimeValue.value,
                    cellValueType = resolveCellValueTypeFromAstType(userType.type, declarationManager)
                )
            )
        }
        is RecordType -> {
            return CellValueType.MemoryReference(
                InMemoryRecord(
                    fields = listOf(
                    )
                )
            )
        }
    }
}
