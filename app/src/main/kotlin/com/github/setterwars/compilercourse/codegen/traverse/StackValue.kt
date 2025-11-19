package com.github.setterwars.compilercourse.codegen.traverse

// What is currently lying on top of the stack
sealed interface StackValue {
    object I32 : StackValue // i32.const
    object F64 : StackValue// f64.const
    data class CellAddress( // internally also plain i32.const
        val cellType: CellType,
    ) : StackValue
}