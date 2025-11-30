package com.github.setterwars.compilercourse.codegen.traverser.cell

import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64ToI32S
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32ToF64S
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue

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
    return when (this) {
        CellValueType.I32Boolean, CellValueType.I32, is CellValueType.MemoryReference -> WasmValue.I32
        CellValueType.F64 -> WasmValue.F64
    }
}

fun adjustStackValue(storageValueType: CellValueType, onStackValueType: CellValueType) = buildList<Instr> {
    if (storageValueType is CellValueType.I32 && onStackValueType is CellValueType.F64) {
        add(F64ToI32S)
    }
    if (storageValueType is CellValueType.F64 &&
        (onStackValueType is CellValueType.I32 || onStackValueType is CellValueType.I32Boolean)
    ) {
        add(I32ToF64S)
    }
}