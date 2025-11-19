package com.github.setterwars.compilercourse.codegen.traverse

fun CodegenData.toStackValue(): StackValue {
    return when (this) {
        is CodegenData.I32 -> StackValue.I32
        is CodegenData.F64 -> StackValue.F64
        else -> StackValue.ObjReference(referencedCodegenData = this)
    }
}