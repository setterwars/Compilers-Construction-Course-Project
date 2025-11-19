package com.github.setterwars.compilercourse.codegen.traverse

import java.util.Stack

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

// Used for expression parsing - what is on the stack after parsing expression
sealed interface StackTopValue {
    object I32 : StackTopValue // i32.const
    object F64 : StackTopValue// f64.const
}

@JvmInline
value class MemoryAddress(val address: Int)

data class ObjReference( // internally also plain i32.const
    val address: MemoryAddress,
    val referencedCodegenData: CodegenData,
) : StackTopValue, CodegenData(bytesSize = 4)