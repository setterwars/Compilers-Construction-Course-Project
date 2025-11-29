package com.github.setterwars.compilercourse.codegen.traverser.ast.declaration

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.traverser.ast.expression.resolveExpression
import com.github.setterwars.compilercourse.codegen.traverser.ast.type.resolveCellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.MemoryReferencable
import com.github.setterwars.compilercourse.codegen.traverser.cell.store
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.name
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclaration
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationNoType
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationWithType

data class VariableDeclarationResolveResult(
    val initializerBlock: Block?, // null for global variables
)

fun WasmContext.resolveVariableDeclaration(
    variableDeclaration: VariableDeclaration
): VariableDeclarationResolveResult {
    return when (variableDeclaration) {
        is VariableDeclarationNoType -> resolveVariableDeclarationNoType(variableDeclaration)
        is VariableDeclarationWithType -> resolveVariableDeclarationWithType(variableDeclaration)
    }
}

fun WasmContext.resolveVariableDeclarationNoType(
    variableDeclarationNoType: VariableDeclarationNoType
): VariableDeclarationResolveResult {
    val er = resolveExpression(variableDeclarationNoType.initialValue)
    val name = variableDeclarationNoType.identifier.name()
    if (declarationManager.inScope()) {
        declarationManager.declareLocalVariable(name, er.onStackValueType)
    } else {
        declarationManager.declareGlobalVariable(name, er.onStackValueType)
    }
    val variable = declarationManager.resolveVariable(name)
    val initializerBlock = Block(
        resultType = er.onStackValueType.toWasmValue(),
        instructions = buildList {
            addAll(variable.store { er.instructions })
        }
    )
    if (declarationManager.inScope()) {
        return VariableDeclarationResolveResult(
            initializerBlock = initializerBlock
        )
    } else {
        declarationManager.addInitializerForGlobalVariable(name, initializerBlock)
    }
    return VariableDeclarationResolveResult(initializerBlock)
}

fun WasmContext.resolveVariableDeclarationWithType(
    variableDeclarationWithType: VariableDeclarationWithType
): VariableDeclarationResolveResult {
    val name = variableDeclarationWithType.identifier.name()
    val cellValueType = resolveCellValueType(variableDeclarationWithType.type)
    if (declarationManager.inScope()) {
        declarationManager.declareLocalVariable(name, cellValueType)
    } else {
        declarationManager.declareGlobalVariable(name, cellValueType)
    }
    val variable = declarationManager.resolveVariable(name)

    val initializerBlock = if (variable.cellValueType is MemoryReferencable) {
        Block(
            resultType = null,
            instructions = createInitializerForMemoryReferencable(variable.cellValueType)
        )
    } else if (variableDeclarationWithType.initialValue != null) {
        val er = resolveExpression(variableDeclarationWithType.initialValue)
        Block(
            resultType = cellValueType.toWasmValue(),
            instructions = buildList {
                addAll(variable.store { er.instructions })
            }
        )
    } else {
        Block(
            null, emptyList()
        )
    }

    if (declarationManager.inScope()) {
        return VariableDeclarationResolveResult(
            initializerBlock = initializerBlock
        )
    } else {
        declarationManager.addInitializerForGlobalVariable(name, initializerBlock)
    }
    return VariableDeclarationResolveResult(initializerBlock)
}

