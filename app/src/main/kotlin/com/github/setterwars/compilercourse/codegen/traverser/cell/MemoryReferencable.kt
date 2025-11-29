package com.github.setterwars.compilercourse.codegen.traverser.cell

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr

sealed interface MemoryReferencable {
    val inMemoryBytesSize: Int
}

/**
 * Arrays are *always* stored in the memory
 * The array is a contiguous and compile-time known number of cells of certain type
 */
data class InMemoryArray(
    val size: Int?,
    val cellValueType: CellValueType
) : MemoryReferencable {
    override val inMemoryBytesSize: Int
        get() = size!! * cellValueType.toWasmValue().bytes
}

/**
 * Records are always stored in the memory
 * The record is a contiguous number of cells of various types
 */
data class InMemoryRecord(
    val fields: List<RecordField>,

    // Initializer - put the address on stack and execute the instructions
    // The record will contain instructions
    // The address WILL BE removed from the stack
    val initializer: List<Instr>?,
) : MemoryReferencable {
    data class RecordField(
        val name: String,
        val cellValueType: CellValueType,
    )

    override val inMemoryBytesSize: Int
        get() = fields.sumOf { it.cellValueType.toWasmValue().bytes }
}