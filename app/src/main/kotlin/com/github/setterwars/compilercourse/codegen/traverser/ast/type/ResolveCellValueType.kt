package com.github.setterwars.compilercourse.codegen.traverser.ast.type

import com.github.setterwars.compilercourse.codegen.traverser.ast.expression.resolveExpression
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryArray
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryRecord
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.CodegenException
import com.github.setterwars.compilercourse.codegen.utils.name
import com.github.setterwars.compilercourse.parser.nodes.ArrayType
import com.github.setterwars.compilercourse.parser.nodes.DeclaredType
import com.github.setterwars.compilercourse.parser.nodes.PrimitiveType
import com.github.setterwars.compilercourse.parser.nodes.RecordType
import com.github.setterwars.compilercourse.parser.nodes.Type
import com.github.setterwars.compilercourse.parser.nodes.UserType
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationNoType
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationWithType
import com.github.setterwars.compilercourse.semantic.CompileTimeInteger

fun WasmContext.resolveCellValueType(
    type: Type,
): CellValueType {
    return when (type) {
        is PrimitiveType -> resolvePrimitiveType(type)
        is UserType -> resolveUserType(type)
        is DeclaredType -> resolveDeclaredType(type)
    }
}

private fun WasmContext.resolvePrimitiveType(
    primitiveType: PrimitiveType
): CellValueType {
    return when (primitiveType) {
        PrimitiveType.INTEGER -> CellValueType.I32
        PrimitiveType.REAL -> CellValueType.F64
        PrimitiveType.BOOLEAN -> CellValueType.I32Boolean
    }
}

private fun WasmContext.resolveDeclaredType(
    declaredType: DeclaredType,
): CellValueType {
    return resolveCellValueType(declaredType.data?.originalType!!)
}

private fun WasmContext.resolveUserType(
    userType: UserType,
): CellValueType {
    return when (userType) {
        is ArrayType -> {
            val arraySize = if (userType.expressionInBrackets != null) {
                userType.expressionInBrackets.data?.compileTimeValue
                        as? CompileTimeInteger ?: throw CodegenException()

            } else {
                null
            }
            return CellValueType.MemoryReference(
                InMemoryArray(
                    size = arraySize?.value,
                    cellValueType = resolveCellValueType(userType.type)
                )
            )
        }

        is RecordType -> {
            // TODO: when variable declaration includes initial value - use it
            return CellValueType.MemoryReference(
                InMemoryRecord(
                    fields = userType.declarations.map { vd ->
                        when (vd) {
                            is VariableDeclarationWithType -> {
                                InMemoryRecord.RecordField(
                                    name = vd.identifier.name(),
                                    cellValueType = resolveCellValueType(vd.type)
                                )
                            }

                            is VariableDeclarationNoType -> {
                                val er = resolveExpression(vd.initialValue)
                                InMemoryRecord.RecordField(
                                    name = vd.identifier.name(),
                                    cellValueType = er.onStackValueType,
                                )
                            }
                        }
                    }
                )
            )
        }
    }
}
