package com.github.setterwars.compilercourse.parser.nodes

import com.github.setterwars.compilercourse.semantic.semanticData.DeclaredTypeSemanticData

sealed interface Type

enum class PrimitiveType : Type { INTEGER, REAL, BOOLEAN }

data class DeclaredType(
    val identifier: Identifier,
) : Type, SemanticDataHolder<DeclaredTypeSemanticData>()

sealed interface UserType : Type

data class ArrayType(
    val expressionInBrackets: Expression?,
    val type: Type,
) : UserType

data class RecordType(
    val declarations: List<VariableDeclaration>,
) : UserType