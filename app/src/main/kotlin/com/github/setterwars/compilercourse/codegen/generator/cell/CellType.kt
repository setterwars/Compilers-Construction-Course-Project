package com.github.setterwars.compilercourse.codegen.generator.cell

/**
 * "Cell" - is any memory in wasm: globals, locals, or main memory
 */
sealed interface CellType {
    data class MemoryCell(val address: Int) : CellType
    data class GlobalsCell(val index: Int) : CellType
    data class LocalsCell(val index: Int) : CellType
}