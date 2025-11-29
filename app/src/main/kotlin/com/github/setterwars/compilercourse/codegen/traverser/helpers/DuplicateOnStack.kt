package com.github.setterwars.compilercourse.codegen.traverser.helpers

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue

fun duplicateOnStack(
    wasmValueOnStack: WasmValue
): List<Instr> = buildList {
    when (wasmValueOnStack) {
        WasmValue.F64 -> {
            
        }
    }
}