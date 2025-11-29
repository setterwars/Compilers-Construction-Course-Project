package com.github.setterwars.compilercourse.codegen.traverser.ast.body

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.parser.nodes.Body

fun WasmContext.resolveBody(body: Body): List<Instr> {
    declarationManager.enterScope()
    declarationManager.exitScope()
    TODO()
}