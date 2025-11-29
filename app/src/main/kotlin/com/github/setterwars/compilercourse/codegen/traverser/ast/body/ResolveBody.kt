package com.github.setterwars.compilercourse.codegen.traverser.ast.body

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.parser.nodes.Body
import com.github.setterwars.compilercourse.parser.nodes.Statement

fun WasmContext.resolveBody(body: Body): List<Instr> {
    declarationManager.enterScope()
    val result = buildList {
        for (bodyElement in body.bodyElements) {
            when (bodyElement) {
                is Statement -> add(resolveState\)
            }
        }
    }
    declarationManager.exitScope()
}