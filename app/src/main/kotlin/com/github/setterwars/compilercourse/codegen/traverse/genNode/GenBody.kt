package com.github.setterwars.compilercourse.codegen.traverse.genNode

import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverse.WasmStructureGenerator
import com.github.setterwars.compilercourse.parser.nodes.Body
import com.github.setterwars.compilercourse.parser.nodes.SimpleDeclaration
import com.github.setterwars.compilercourse.parser.nodes.Statement

fun WasmStructureGenerator.genBody(body: Body): List<Instr> {
    return buildList {
        for (bodyElem in body.bodyElements) {
            when (bodyElem) {
                is SimpleDeclaration -> genSimpleDeclaration(bodyElem)
                is Statement -> addAll(genStatement(bodyElem))
            }
        }
    }
}