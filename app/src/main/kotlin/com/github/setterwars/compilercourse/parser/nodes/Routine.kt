package com.github.setterwars.compilercourse.parser.nodes

data class RoutineDeclaration(
    val header: RoutineHeader,
    val body: RoutineBody?,
) : Declaration

data class RoutineHeader(
    val name: Identifier,
    val parameters: Parameters,
    val returnType: Type?,
)

sealed interface RoutineBody

data class FullRoutineBody(
    val body: Body,
)

data class SingleExpressionBody(
    val expression: Expression,
)

data class Parameters(
    val parameters: List<ParameterDeclaration>,
)

data class ParameterDeclaration(
    val name: Identifier,
    val type: Type,
)
