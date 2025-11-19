package com.github.setterwars.compilercourse.codegen.traverse

// What can be stored inside stack
sealed interface StackValue {
    object I32 : StackValue // i32.const
    object F64 : StackValue// f64.const
    data class ObjReference( // internally also plain i32.const
        val referencedCodegenData: CodegenData,
    ) : StackValue
}