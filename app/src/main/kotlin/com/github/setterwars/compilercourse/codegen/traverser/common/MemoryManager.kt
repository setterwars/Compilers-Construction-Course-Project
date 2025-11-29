package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType

class MemoryManager {
    private val cells = mutableListOf<CellValueType>()

    var currentAddressPointer = 13
        private set

    fun addCell(cellType: CellValueType) {
        val cellSize = when (cellType) {
            CellValueType.I32 -> 4
            CellValueType.F64 -> 8
            CellValueType.I32Boolean -> 4
            is CellValueType.MemoryReference -> 4
        }
        currentAddressPointer += cellSize
        cells.add(cellType)
    }

    companion object {
        const val RESERVED_I32_ADDR = 1
        const val RESERVED_F64_ADDR = 5
    }

}