package com.github.setterwars.compilercourse.codegen.traverser.cell

import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.GlobalGet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.GlobalSet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.LocalGet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.LocalSet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.MemoryManager

/**
 * "Variable" - is anything that has a) name b) reserved place in some cell (globals, locals, memory)
 * For example, function parameter is a variable that is defined in the locals
 */
data class Variable(
    val name: String,
    val cellValueType: CellValueType,
    val variableType: VariableType,
)

sealed interface VariableType {
    data class Framed(val frameOffset: Int) : VariableType
    data class Local(val index: Int) : VariableType
    data class Global(val index: Int) : VariableType
}

// Generate instructions, that will store the value into the variable
// NOTE: this function DOES NOT perform any semantic analysis, and expects
// the value on the stack to MATCH the wasm value of the variable
fun Variable.store(
    putValueOnStack: () -> List<Instr>
): List<Instr> = buildList {
    when (variableType) {
        is VariableType.Framed -> {
            addAll(MemoryManager.addressForOffset(variableType.frameOffset))
            addAll(putValueOnStack())
            when (cellValueType.toWasmValue()) {
                WasmValue.I32 -> add(I32Store())
                WasmValue.F64 -> add(F64Store())
            }
        }
        is VariableType.Local -> {
            addAll(putValueOnStack())
            add(LocalSet(variableType.index))
        }
        is VariableType.Global -> {
            addAll(putValueOnStack())
            add(GlobalSet(variableType.index))
        }
    }
}

fun Variable.store(
    putValueOnStack: List<Instr>
): List<Instr> {
    return store { putValueOnStack }
}

// Generate instructions that will load the value of the variable onto the stack
fun Variable.load(): List<Instr> = buildList {
    when (variableType) {
        is VariableType.Framed -> {
            addAll(MemoryManager.addressForOffset(variableType.frameOffset))
            when (cellValueType.toWasmValue()) {
                WasmValue.I32 -> { add(I32Load()) }
                WasmValue.F64 -> { add(F64Load()) }
            }
        }
        is VariableType.Local -> {
            add(LocalGet(variableType.index))
        }
        is VariableType.Global -> {
            add(GlobalGet(variableType.index))
        }
    }
}