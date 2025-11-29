package com.github.setterwars.compilercourse.codegen.traverser.ast.program

import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmDefinition
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmFunc
import com.github.setterwars.compilercourse.codegen.traverser.ast.declaration.resolveVariableDeclaration
import com.github.setterwars.compilercourse.codegen.traverser.ast.routine.resolveRoutineDeclaration
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.parser.nodes.Program
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.TypeDeclaration
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclaration

fun WasmContext.resolveProgram(program: Program): List<WasmDefinition> = buildList {
    val routines = mutableListOf<WasmFunc>()
    for (declaration in program.declarations) {
        when (declaration) {
            is VariableDeclaration -> {
                resolveVariableDeclaration(declaration)
            }
            is RoutineDeclaration -> {
                resolveRoutineDeclaration(declaration)?.let {
                    routines.add(it)
                }
            }
            is TypeDeclaration -> {} // Type declarations are handled during semantic pass
        }
    }
    routines.sortBy { declarationManager.resolveRoutine(it.name!!).index }
    for (routine in routines) {
        add(routine)
    }
}