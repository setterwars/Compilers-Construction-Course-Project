package com.github.setterwars.compilercourse.codegen.traverse

sealed class CellType(val bytesSize: Int) {
    object I32 : CellType(bytesSize = 4)
    object F64 : CellType(bytesSize = 8)
    data class ArrayReference(
        val count: Int,
        val cellType: CellType,
    ) : CellType(bytesSize = count * cellType.bytesSize)
    data class Record(
        val fields: List<Pair<String, CellType>>
    ) : CellType(bytesSize = fields.sumOf { it.second.bytesSize })
}

// What can be stored in memory
sealed class CodegenData(val bytesSize: Int) {
    object I32 : CodegenData(bytesSize = 4)

    object F64 : CodegenData(bytesSize = 8)

    data class Array(
        val count: Int,
        val elementsData: CodegenData
    ) : CodegenData(
        count * (if (elementsData.isPrimitive()) elementsData.bytesSize else 4)
    )

    data class Record(
        val fields: List<Pair<String, CodegenData>>
    ) : CodegenData(fields.sumOf { if (it.second.isPrimitive()) it.second.bytesSize else 4 })

    fun isPrimitive() = this is I32 || this is F64
}