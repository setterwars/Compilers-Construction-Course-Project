package com.github.setterwars.compilercourse.codegen.traverser.ast.body

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverser.ast.declaration.resolveVariableDeclaration
import com.github.setterwars.compilercourse.codegen.traverser.ast.statement.resolveStatement
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.parser.nodes.Body
import com.github.setterwars.compilercourse.parser.nodes.Statement
import com.github.setterwars.compilercourse.parser.nodes.TypeDeclaration
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclaration

fun WasmContext.resolveBody(body: Body): List<Instr> {
    declarationManager.enterScope()
    val result = buildList<Instr> {
        for (bodyElement in body.bodyElements) {
            when (bodyElement) {
                is Statement -> add(resolveStatement(bodyElement))
                is VariableDeclaration -> add(resolveVariableDeclaration(bodyElement).initializerBlock!!)
                is TypeDeclaration -> {}
            }
        }
    }
    declarationManager.exitScope()
    return result
}