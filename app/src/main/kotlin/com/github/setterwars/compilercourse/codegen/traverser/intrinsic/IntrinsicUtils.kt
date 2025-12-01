package com.github.setterwars.compilercourse.codegen.traverser.intrinsic

import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.FuncType
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.LocalGet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmFunc
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.MemoryManager

const val ALLOCATE_I32_ARRAY_FUNCTION_NAME = "__allocate_i32_array"
const val ALLOCATE_F64_ARRAY_FUNCTION_NAME = "__allocate_f64_array"
const val WRITE_I32_TO_ARRAY_FUNCTION_NAME = "__write_i32_to_array"
const val WRITE_F64_TO_ARRAY_FUNCTION_NAME = "__write_f64_to_array"

fun getAllocatingFunction(wasmValue: WasmValue, name: String): WasmFunc {
    return WasmFunc(
        type = FuncType(
            params = listOf(WasmValue.I32), // number of cells
            results = listOf(WasmValue.I32), // resulted address
        ),
        locals = emptyList(),
        body = buildList {
            add(LocalGet(0))
            add(I32Const(wasmValue.bytes))
            add(I32Binary(I32BinOp.Mul))
            addAll(MemoryManager.allocateBytesRuntime())
        },
        name = name
    )
}

fun getWriteWasmValueFunction(wasmValue: WasmValue, name: String): WasmFunc {
    return WasmFunc(
        type = FuncType(
            params = listOf(WasmValue.I32, WasmValue.I32, wasmValue), // arrayAddress, index (0-indexed), value
            results = emptyList()
        ),
        locals = emptyList(),
        body = buildList {
            add(LocalGet(0))
            add(LocalGet(1))
            add(I32Const(wasmValue.bytes))
            add(I32Binary(I32BinOp.Mul))
            add(I32Binary(I32BinOp.Add))
            add(LocalGet(2))
            when (wasmValue) {
                WasmValue.I32 -> add(I32Store())
                WasmValue.F64 -> add(F64Store())
            }
        },
        name = name,
    )
}
