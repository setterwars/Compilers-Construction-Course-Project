package com.github.setterwars.compilercourse.parser.nodes

data class Program(
    val declarations: List<Declaration>,
)

sealed interface Declaration

sealed interface SimpleDeclaration : Declaration

sealed interface VariableDeclaration : SimpleDeclaration

data class VariableDeclarationWithType(
    val identifier: Identifier,
    val type: Type,
    val initialValue: Expression?,
) : VariableDeclaration

data class VariableDeclarationNoType(
    val identifier: Identifier,
    val initialValue: Expression,
) : VariableDeclaration

data class TypeDeclaration(
    val identifier: Identifier,
    val type: Type,
) : SimpleDeclaration