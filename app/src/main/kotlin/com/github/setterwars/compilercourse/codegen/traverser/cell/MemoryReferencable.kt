package com.github.setterwars.compilercourse.codegen.traverser.cell

import com.github.setterwars.compilercourse.parser.nodes.Expression

sealed interface MemoryReferencable {
    val inMemoryBytesSize: Int
}

/**
 * Arrays are *always* stored in the memory
 * The array is a contiguous number of cells of certain type
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
) : MemoryReferencable {
    data class RecordField(
        val name: String,
        val cellValueType: CellValueType,
        val initialValue: Expression?,
    )

    override val inMemoryBytesSize: Int
        get() = fields.sumOf { it.cellValueType.toWasmValue().bytes }
}