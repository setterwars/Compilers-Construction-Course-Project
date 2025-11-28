package com.github.setterwars.compilercourse.codegen.generator.cell

import com.github.setterwars.compilercourse.codegen.ir.WasmValue

/**
 * "Cell" - is any memory in wasm: globals, locals, or main memory
 */
sealed interface CellValueType {
    object I32 : CellValueType
    object F64 : CellValueType
    object I32Boolean : CellValueType
    data class MemoryReference(
        val referencable: MemoryReferencable
    ) : CellValueType
}

fun CellValueType.toWasmValue(): WasmValue {
    when (this) {
        CellValueType.I32Boolean, CellValueType.I32, is CellValueType.MemoryReference -> WasmValue.I32
        CellValueType.F64 -> WasmValue.F64
    }
}
