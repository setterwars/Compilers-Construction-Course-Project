package com.github.setterwars.compilercourse.codegen.traverse

sealed class CellType(val bytesSize: Int) {
    object I32 : CellType(bytesSize = 4)
    object F64 : CellType(bytesSize = 8)
    data class ArrayReference(
        val memArray: MemArray
    ) : CellType(bytesSize = 4)
    data class RecordReference(
        val memRecord: MemRecord
    ) : CellType(bytesSize = 4)
}

data class MemArray(
    val count: Int,
    val cellType: CellType
)

data class MemRecord(
    val fields: List<Pair<String, CellType>>
)
