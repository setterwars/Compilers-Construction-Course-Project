package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block

class MainFunctionManager {
    private val blocks = mutableListOf<Block>()

    fun addBlock(block: Block) {
        blocks.add(block)
    }
}