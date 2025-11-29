package com.github.setterwars.compilercourse.codegen.traverser.cell

import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.GlobalGet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.LocalGet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.LocalSet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue

/**
 * "Variable" - is anything that has a) name b) reserved place in some cell (globals, locals, memory)
 * For example, function parameter is a variable that is defined in the locals
 */
data class Variable(
    val cellType: CellType,
    val cellValueType: CellValueType,
    val name: String, // empty = no name
)

// Generate instructions, that will store the value into the variable
fun Variable.store(putValueOnStackInstructions: List<Instr>): List<Instr> = buildList {
    when (cellType) {
        is CellType.MemoryCell -> {
            add(I32Const(cellType.address!!))
            addAll(putValueOnStackInstructions)
            when (cellValueType.toWasmValue()) {
                WasmValue.I32 -> add(I32Store())
                WasmValue.F64 -> add(F64Store())
            }
        }
        is CellType.LocalsCell -> {
            addAll(putValueOnStackInstructions)
            add(LocalSet(cellType.index))
        }
        is CellType.GlobalsCell -> {
            addAll(putValueOnStackInstructions)
            add(GlobalGet(cellType.index))
        }
    }
}

// Generate instructions that will load the value of the variable onto the stack
fun Variable.load(): List<Instr> = buildList {
    when (cellType) {
        is CellType.MemoryCell -> {
            when (cellValueType.toWasmValue()) {
                WasmValue.I32 -> {
                    add(I32Const(cellType.address))
                    add(I32Load())
                }
                WasmValue.F64 -> {
                    add(I32Const(cellType.address))
                    add(F64Load())
                }
            }
        }
        is CellType.LocalsCell -> {
            add(LocalGet(cellType.index))
        }
        is CellType.GlobalsCell -> {
            add(GlobalGet(cellType.index))
        }
    }
}