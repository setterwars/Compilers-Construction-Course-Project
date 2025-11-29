package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue

class MemoryManager {
    companion object {
        const val MEMORY_POINTER_ADDR = 1
        const val FRAME_L_ADDR = MEMORY_POINTER_ADDR + 4
        const val FRAME_R_ADDR = FRAME_L_ADDR + 4
        const val RESERVED_F64_ADDR = FRAME_R_ADDR + 4
        const val RESERVED_I32_ADDR = RESERVED_F64_ADDR + 8
        const val MEMORY_BEGIN = RESERVED_I32_ADDR + 4

        // Generate instructions that will put on the stack value of the offset relative to
        // the current value of frame L pointer
        fun addressForOffset(offset: Int): List<Instr> = buildList {
            add(I32Const(FRAME_L_ADDR))
            add(I32Load())
            add(I32Const(offset))
            add(I32Binary(I32BinOp.Add))
        }

        // Allocate space for contiguous n bytes
        // Put the address of the first byte on the stack
        fun allocateBytes(n: Int): List<Instr> = buildList {
            add(I32Const(MEMORY_POINTER_ADDR))
            add(I32Load())

            add(I32Const(MEMORY_POINTER_ADDR))
            add(I32Const(MEMORY_POINTER_ADDR))
            add(I32Load())
            add(I32Const(n))
            add(I32Binary(I32BinOp.Add))
            add(I32Store())
        }

        fun moveRFrameForCellValueType(cellValueType: CellValueType) = buildList<Instr> {
            add(I32Const(FRAME_R_ADDR))
            add(I32Const(FRAME_R_ADDR))
            add(I32Load())
            add(I32Const(cellValueType.toWasmValue().bytes))
            add(I32Binary(I32BinOp.Add))
            add(I32Store())
        }
    }
}