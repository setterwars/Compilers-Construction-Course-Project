package com.github.setterwars.compilercourse.codegen.generator.cell

import com.github.setterwars.compilercourse.codegen.generator.common.ScopedEntitiesManager

sealed interface MemoryReferencable

/**
 * Arrays are *always* stored in the memory
 * The array is a contiguous and compile-time known number of cells of certain type
 */
data class InMemoryArray(
    val size: Int,
    val cellValueType: CellValueType
) : MemoryReferencable

/**
 * Records are always stored in the memory
 * The record is a contiguous number of cells of various types
 */
data class InMemoryRecord(
    val fields: List<RecordField>,
    val initializer: (address: Int) -> Unit,
) : MemoryReferencable {
    data class RecordField(
        val name: String,
        val cellValueType: CellValueType,
    )
}