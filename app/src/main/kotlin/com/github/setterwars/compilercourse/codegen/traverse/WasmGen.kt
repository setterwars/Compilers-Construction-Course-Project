package com.github.setterwars.compilercourse.codegen.traverse

import com.github.setterwars.compilercourse.codegen.ir.WasmModule
import com.github.setterwars.compilercourse.parser.nodes.Program
import com.github.setterwars.compilercourse.semantic.SemanticInfoStore

class WasmStructureGenerator(val semanticInfoStore: SemanticInfoStore) {

   val memoryManager = MemoryManager() // we have single strip of memory
    val declarationManager = DeclarationManager(memoryManager) // symbol table basically

    fun traverse(program: Program): WasmModule {
        TODO()
    }
}