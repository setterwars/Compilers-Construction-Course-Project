package com.github.setterwars.compilercourse.codegen.encoder

import com.github.setterwars.compilercourse.codegen.ir.Block
import com.github.setterwars.compilercourse.codegen.ir.Br
import com.github.setterwars.compilercourse.codegen.ir.BrIf
import com.github.setterwars.compilercourse.codegen.ir.Call
import com.github.setterwars.compilercourse.codegen.ir.Drop
import com.github.setterwars.compilercourse.codegen.ir.F64BinOp
import com.github.setterwars.compilercourse.codegen.ir.F64Binary
import com.github.setterwars.compilercourse.codegen.ir.F64Compare
import com.github.setterwars.compilercourse.codegen.ir.F64Const
import com.github.setterwars.compilercourse.codegen.ir.F64Load
import com.github.setterwars.compilercourse.codegen.ir.F64RelOp
import com.github.setterwars.compilercourse.codegen.ir.F64Store
import com.github.setterwars.compilercourse.codegen.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.ir.I32Compare
import com.github.setterwars.compilercourse.codegen.ir.I32Const
import com.github.setterwars.compilercourse.codegen.ir.I32Load
import com.github.setterwars.compilercourse.codegen.ir.I32RelOp
import com.github.setterwars.compilercourse.codegen.ir.I32Store
import com.github.setterwars.compilercourse.codegen.ir.If
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.ir.LocalGet
import com.github.setterwars.compilercourse.codegen.ir.LocalSet
import com.github.setterwars.compilercourse.codegen.ir.LocalTee
import com.github.setterwars.compilercourse.codegen.ir.Loop
import com.github.setterwars.compilercourse.codegen.ir.Nop
import com.github.setterwars.compilercourse.codegen.ir.Return
import com.github.setterwars.compilercourse.codegen.ir.Unreachable
import com.github.setterwars.compilercourse.codegen.ir.ValueType

class WasmWriter {
    private val buf = mutableListOf<Byte>()

    fun u8(v: Int) {
        buf += v.toByte()
    }

    fun writeRaw(bytes: ByteArray) {
        buf.addAll(bytes.asList())
    }

    fun writePreamble() {
        // 00 61 73 6D = "\0asm"
        // 01 00 00 00 = version 1
        u8(0x00); u8(0x61); u8(0x73); u8(0x6D)
        u8(0x01); u8(0x00); u8(0x00); u8(0x00)
    }

    // Unsigned LEB128 for u32
    fun writeU32(value: Int) {
        var v = value
        do {
            var byte = v and 0x7F
            v = v ushr 7
            if (v != 0) {
                byte = byte or 0x80
            }
            u8(byte)
        } while (v != 0)
    }

    // Signed LEB128 for i32 (for i32.const)
    fun writeS32(value: Int) {
        var v = value
        var more = true
        while (more) {
            var byte = v and 0x7F
            v = v shr 7
            val signBitSet = (byte and 0x40) != 0
            if ((v == 0 && !signBitSet) || (v == -1 && signBitSet)) {
                more = false
            } else {
                byte = byte or 0x80
            }
            u8(byte)
        }
    }

    // Little-endian IEEE-754 f64
    fun writeF64(value: Double) {
        val raw = java.lang.Double.doubleToRawLongBits(value)
        for (i in 0 until 8) {
            u8(((raw ushr (8 * i)) and 0xFF).toInt())
        }
    }

    fun writeName(name: String) {
        val bytes = name.toByteArray(Charsets.UTF_8)
        writeU32(bytes.size)
        writeRaw(bytes)
    }

    fun writeValType(type: ValueType) {
        val code = when (type) {
            ValueType.I32 -> 0x7F
            ValueType.F64 -> 0x7C
        }
        u8(code)
    }

    fun writeBlockType(type: ValueType?) {
        if (type == null) {
            u8(0x40) // block with no result
        } else {
            writeValType(type)
        }
    }

    fun writeMemArg(align: Int, offset: Int) {
        writeU32(align)
        writeU32(offset)
    }

    fun <T> writeVec(items: List<T>, writeItem: WasmWriter.(T) -> Unit) {
        writeU32(items.size)
        for (it in items) writeItem(it)
    }

    fun section(id: Int, buildPayload: WasmWriter.() -> Unit) {
        u8(id)
        val payloadWriter = WasmWriter()
        payloadWriter.buildPayload()
        val payloadBytes = payloadWriter.toByteArray()
        writeU32(payloadBytes.size)
        writeRaw(payloadBytes)
    }

    fun toByteArray(): ByteArray = buf.toByteArray()
}

fun WasmWriter.writeInstr(instr: Instr) {
    when (instr) {
        is Unreachable -> u8(0x00)
        is Nop -> u8(0x01)

        is Block -> {
            u8(0x02) // block
            writeBlockType(instr.resultType)
            instr.instructions.forEach { writeInstr(it) }
            u8(0x0B) // end
        }

        is Loop -> {
            u8(0x03) // loop
            writeBlockType(instr.resultType)
            instr.instructions.forEach { writeInstr(it) }
            u8(0x0B) // end
        }

        is If -> {
            u8(0x04) // if
            writeBlockType(instr.resultType)
            instr.thenInstrs.forEach { writeInstr(it) }
            if (instr.elseInstrs.isNotEmpty()) {
                u8(0x05) // else
                instr.elseInstrs.forEach { writeInstr(it) }
            }
            u8(0x0B) // end
        }

        is Br -> {
            u8(0x0C) // br
            writeU32(instr.depth)
        }

        is BrIf -> {
            u8(0x0D) // br_if
            writeU32(instr.depth)
        }

        is Return -> u8(0x0F)
        is Drop -> u8(0x1A)

        is Call -> {
            u8(0x10) // call
            writeU32(instr.funcIndex)
        }

        is LocalGet -> {
            u8(0x20) // local.get
            writeU32(instr.index)
        }

        is LocalSet -> {
            u8(0x21) // local.set
            writeU32(instr.index)
        }

        is LocalTee -> {
            u8(0x22) // local.tee
            writeU32(instr.index)
        }

        is I32Load -> {
            u8(0x28) // i32.load
            writeMemArg(instr.align, instr.offset)
        }

        is F64Load -> {
            u8(0x2B) // f64.load
            writeMemArg(instr.align, instr.offset)
        }

        is I32Store -> {
            u8(0x36) // i32.store
            writeMemArg(instr.align, instr.offset)
        }

        is F64Store -> {
            u8(0x39) // f64.store
            writeMemArg(instr.align, instr.offset)
        }

        is I32Const -> {
            u8(0x41) // i32.const
            writeS32(instr.value)
        }

        is F64Const -> {
            u8(0x44) // f64.const
            writeF64(instr.value)
        }

        is I32Binary -> {
            val op = when (instr.op) {
                I32BinOp.Add -> 0x6A   // i32.add
                I32BinOp.Sub -> 0x6B   // i32.sub
                I32BinOp.Mul -> 0x6C   // i32.mul
                I32BinOp.DivS -> 0x6D  // i32.div_s
                I32BinOp.DivU -> 0x6E  // i32.div_u
            }
            u8(op)
        }

        is F64Binary -> {
            val op = when (instr.op) {
                F64BinOp.Add -> 0xA0   // f64.add
                F64BinOp.Sub -> 0xA1   // f64.sub
                F64BinOp.Mul -> 0xA2   // f64.mul
                F64BinOp.Div -> 0xA3   // f64.div
            }
            u8(op)
        }

        is I32Compare -> {
            val op = when (instr.op) {
                I32RelOp.Eq  -> 0x46   // i32.eq
                I32RelOp.Ne  -> 0x47   // i32.ne
                I32RelOp.LtS -> 0x48   // i32.lt_s
                I32RelOp.LtU -> 0x49   // i32.lt_u
                I32RelOp.GtS -> 0x4A   // i32.gt_s
                I32RelOp.GtU -> 0x4B   // i32.gt_u
                I32RelOp.LeS -> 0x4C   // i32.le_s
                I32RelOp.LeU -> 0x4D   // i32.le_u
                I32RelOp.GeS -> 0x4E   // i32.ge_s
                I32RelOp.GeU -> 0x4F   // i32.ge_u
            }
            u8(op)
        }

        is F64Compare -> {
            val op = when (instr.op) {
                F64RelOp.Eq -> 0x61    // f64.eq
                F64RelOp.Ne -> 0x62    // f64.ne
                F64RelOp.Lt -> 0x63    // f64.lt
                F64RelOp.Gt -> 0x64    // f64.gt
                F64RelOp.Le -> 0x65    // f64.le
                F64RelOp.Ge -> 0x66    // f64.ge
            }
            u8(op)
        }
    }
}