package com.github.setterwars.compilercourse.parser.nodes

sealed interface Statement : BodyElement

data class Assignment(
    val modifiablePrimary: ModifiablePrimary,
    val expression: Expression,
) : Statement

data class RoutineCall(
    val routineName: Identifier,
    val arguments: List<RoutineCallArgument>,
) : Statement

data class RoutineCallArgument(
    val expression: Expression,
)

data class WhileLoop(
    val condition: Expression,
    val body: Body,
) : Statement

data class ForLoop(
    val loopVariable: Identifier,
    val range: Range,
    val reverse: Boolean,
    val body: Body,
) : Statement

data class Range(
    val begin: Expression,
    val end: Expression?,
)

data class IfStatement(
    val condition: Expression,
    val thenBody: Body,
    val elseBody: Body?,
) : Statement

data class PrintStatement(
    val expression: Expression,
    val rest: List<Expression>,
) : Statement