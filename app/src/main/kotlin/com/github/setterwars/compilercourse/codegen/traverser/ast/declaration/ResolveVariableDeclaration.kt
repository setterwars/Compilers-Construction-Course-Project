package com.github.setterwars.compilercourse.codegen.traverser.ast.declaration

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverser.ast.expression.resolveExpression
import com.github.setterwars.compilercourse.codegen.traverser.ast.type.resolveCellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.MemoryReferencable
import com.github.setterwars.compilercourse.codegen.traverser.cell.adjustStackValue
import com.github.setterwars.compilercourse.codegen.traverser.cell.store
import com.github.setterwars.compilercourse.codegen.traverser.common.MemoryManager
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
    val initializerInstructions = mutableListOf<Instr>()
    if (declarationManager.inScope()) {
        declarationManager.declareLocalVariable(name, er.onStackValueType)
        initializerInstructions.addAll(MemoryManager.moveRFrameForCellValueType(er.onStackValueType))
    } else {
        declarationManager.declareGlobalVariable(name, er.onStackValueType)
    }
    val variable = declarationManager.resolveVariable(name)
    val putValueInitialValueOnStack = buildList {
        addAll(er.instructions)
        addAll(adjustStackValue(variable.cellValueType, er.onStackValueType))
    }
    initializerInstructions.addAll(variable.store(putValueInitialValueOnStack))
    val initializerBlock = Block(
        null,
        instructions = initializerInstructions,
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
    val initializerInstructions = mutableListOf<Instr>()
    if (declarationManager.inScope()) {
        declarationManager.declareLocalVariable(name, cellValueType)
        initializerInstructions.addAll(MemoryManager.moveRFrameForCellValueType(cellValueType))
    } else {
        declarationManager.declareGlobalVariable(name, cellValueType)
    }
    val variable = declarationManager.resolveVariable(name)

    val putValueInitialValueOnStack: List<Instr> = buildList {
        if (variable.cellValueType is CellValueType.MemoryReference) {
            addAll(createInitializerForMemoryReferencable(variable.cellValueType.referencable))
        } else if (variableDeclarationWithType.initialValue != null) {
            val er = resolveExpression(variableDeclarationWithType.initialValue)
            addAll(er.instructions)
            addAll(adjustStackValue(variable.cellValueType, er.onStackValueType))
        }
    }
    initializerInstructions.addAll(variable.store(putValueInitialValueOnStack))
    val initializerBlock = Block(
        null,
        initializerInstructions,
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

