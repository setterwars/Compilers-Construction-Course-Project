package com.github.setterwars.compilercourse.codegen.traverse

// Suppose that the stack contains whatever this cell contains
// Then what is the type of object the stack contains
fun CellType.toStackValue(): StackValue {
    return when (this) {
        is CellType.I32 -> StackValue.I32
        is CellType.F64 -> StackValue.F64
        else -> StackValue.CellAddress(cellType = this)
    }
}