package com.github.setterwars.compilercourse.codegen.traverse

// Literally what is stored in the memory
sealed class CodegenData(val bytesSize: Int) {
    object I32 : CodegenData(bytesSize = 4)

    object F64 : CodegenData(bytesSize = 8)

    data class Array(
        val count: Int,
        val elementsData: CodegenData
    ) : CodegenData(count * elementsData.bytesSize)

    data class Record(
        val fields: List<Pair<String, CodegenData>>
    ) : CodegenData(fields.sumOf { it.second.bytesSize })
}

class CodegenVariable(
    val address: Int,
    val codegenData: CodegenData
)
