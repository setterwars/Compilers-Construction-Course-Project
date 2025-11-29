package com.github.setterwars.compilercourse.parser.nodes

import com.github.setterwars.compilercourse.semantic.semanticData.RoutineDeclarationSemanticData

data class RoutineDeclaration(
    val header: RoutineHeader,
    val body: RoutineBody?,
) : Declaration, SemanticDataHolder<RoutineDeclarationSemanticData>()

data class RoutineHeader(
    val name: Identifier,
    val parameters: Parameters,
    val returnType: Type?,
)

sealed interface RoutineBody

data class FullRoutineBody(
    val body: Body,
) : RoutineBody

data class SingleExpressionBody(
    val expression: Expression,
): RoutineBody

data class Parameters(
    val parameters: List<ParameterDeclaration>,
)

data class ParameterDeclaration(
    val name: Identifier,
    val type: Type,
)
