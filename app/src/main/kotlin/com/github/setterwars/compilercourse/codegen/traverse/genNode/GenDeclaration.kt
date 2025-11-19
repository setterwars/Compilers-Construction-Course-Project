package com.github.setterwars.compilercourse.codegen.traverse.genNode

import com.github.setterwars.compilercourse.codegen.traverse.WasmStructureGenerator
import com.github.setterwars.compilercourse.parser.nodes.SimpleDeclaration
import com.github.setterwars.compilercourse.parser.nodes.TypeDeclaration
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclaration
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationNoType
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationWithType

fun WasmStructureGenerator.genSimpleDeclaration(
    simpleDeclaration: SimpleDeclaration
) {
    return when (simpleDeclaration) {
        is VariableDeclaration -> genVariableDeclaration(simpleDeclaration)
        is TypeDeclaration -> genTypeDeclaration(simpleDeclaration)
    }
}

fun WasmStructureGenerator.genVariableDeclaration(
    variableDeclaration: VariableDeclaration
) {
    return when (variableDeclaration) {
        is VariableDeclarationWithType ->
            genVariableDeclarationWithType(variableDeclaration)
        is VariableDeclarationNoType ->
            getVariableDeclarationNoType(variableDeclaration)
    }
}

fun WasmStructureGenerator.genTypeDeclaration(typeDeclaration: TypeDeclaration) {
    declarationManager.declareType(
        name = typeDeclaration.identifier.token.lexeme,
        cellType = resolveCellTypeFromType(typeDeclaration.type)
    )
}

fun WasmStructureGenerator.genVariableDeclarationWithType(
    variableDeclarationWithType: VariableDeclarationWithType
) {
    declarationManager.declareVariable(
        name = variableDeclarationWithType.identifier.token.lexeme,
        cellType = resolveCellTypeFromType(variableDeclarationWithType.type)
    )
}

fun WasmStructureGenerator.getVariableDeclarationNoType(
    variableDeclarationNoType: VariableDeclarationNoType
) {
    TODO("Implement default values for variables")
}