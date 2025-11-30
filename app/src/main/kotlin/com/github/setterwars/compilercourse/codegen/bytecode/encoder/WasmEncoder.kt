package com.github.setterwars.compilercourse.codegen.bytecode.encoder

import com.github.setterwars.compilercourse.codegen.bytecode.ir.ExportKind
import com.github.setterwars.compilercourse.codegen.bytecode.ir.FuncType
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmExport
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmFunc
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmMemory
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmModule
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmGlobal
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmI32Global
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmF64Global

object WasmEncoder {

    fun encode(module: WasmModule): ByteArray {
        val writer = WasmWriter()

        writer.writePreamble()

        val funcs    = module.definitions.filterIsInstance<WasmFunc>()
        val memories = module.definitions.filterIsInstance<WasmMemory>()
        val exports  = module.definitions.filterIsInstance<WasmExport>()
        val globals  = module.definitions.filterIsInstance<WasmGlobal>()   // <-- ADDED

        // --- Type section (id = 1) ---
        val funcTypes = funcs.map { it.type }.distinct()
        if (funcTypes.isNotEmpty()) {
            writer.section(1) {
                writeVec(funcTypes) { type ->
                    u8(0x60) // functype
                    writeVec(type.params) { writeValType(it) }
                    writeVec(type.results) { writeValType(it) }
                }
            }
        }

        // Map FuncType -> type index
        val funcTypeIndex: Map<FuncType, Int> =
            funcTypes.withIndex().associate { indexed ->
                indexed.value to indexed.index
            }

        // --- Function section (id = 3) ---
        if (funcs.isNotEmpty()) {
            writer.section(3) {
                writeVec(funcs) { fn ->
                    writeU32(funcTypeIndex.getValue(fn.type))
                }
            }
        }

        // --- Memory section (id = 5) ---
        if (memories.isNotEmpty()) {
            writer.section(5) {
                writeVec(memories) { mem ->
                    if (mem.maxPages == null) {
                        u8(0x00)           // limits: min only
                        writeU32(mem.minPages)
                    } else {
                        u8(0x01)           // limits: min + max
                        writeU32(mem.minPages)
                        writeU32(mem.maxPages)
                    }
                }
            }
        }

        // --- Global section (id = 6) ---  <-- ADDED
        if (globals.isNotEmpty()) {
            writer.section(6) {
                writeVec(globals) { g ->
                    when (g) {
                        is WasmI32Global -> {
                            // valtype
                            writeValType(WasmValue.I32)
                            // mutability: 0 = const, 1 = mut
                            u8(if (g.mutable) 0x01 else 0x00)
                            // init expr: i32.const <value> end
                            u8(0x41) // i32.const
                            writeS32(g.initValue)
                            u8(0x0B) // end
                        }
                        is WasmF64Global -> {
                            writeValType(WasmValue.F64)
                            u8(if (g.mutable) 0x01 else 0x00)
                            // init expr: f64.const <value> end
                            u8(0x44) // f64.const
                            writeF64(g.initValue)
                            u8(0x0B) // end
                        }
                    }
                }
            }
        }

        // --- Export section (id = 7) ---
        if (exports.isNotEmpty()) {
            writer.section(7) {
                writeVec(exports) { ex ->
                    writeName(ex.name)
                    when (ex.kind) {
                        ExportKind.Func -> {
                            u8(0x00) // func
                            writeU32(ex.index)
                        }
                        ExportKind.Memory -> {
                            u8(0x02) // memory
                            writeU32(ex.index)
                        }
                    }
                }
            }
        }

        // --- Start section (id = 8) ---
        val startIndex = funcs.indexOfFirst { it.isStart }
        if (startIndex >= 0) {
            writer.section(8) {
                writeU32(startIndex)
            }
        }

        // --- Code section (id = 10) ---
        if (funcs.isNotEmpty()) {
            writer.section(10) {
                writeVec(funcs) { fn ->
                    val bodyWriter = WasmWriter()

                    // locals (non-params), grouped by type to match Wasm's local decl form
                    val localGroups = mutableListOf<Pair<WasmValue, Int>>()
                    for (localType in fn.locals) {
                        if (localGroups.isNotEmpty() &&
                            localGroups.last().first == localType
                        ) {
                            val lastIndex = localGroups.lastIndex
                            val last = localGroups[lastIndex]
                            localGroups[lastIndex] = last.first to (last.second + 1)
                        } else {
                            localGroups += localType to 1
                        }
                    }

                    // vec(local) where each entry is (count, valtype)
                    bodyWriter.writeU32(localGroups.size)
                    for ((type, count) in localGroups) {
                        bodyWriter.writeU32(count)
                        bodyWriter.writeValType(type)
                    }

                    // instruction sequence
                    fn.body.forEach { instr ->
                        bodyWriter.writeInstr(instr)
                    }
                    bodyWriter.u8(0x0B) // end

                    val bodyBytes = bodyWriter.toByteArray()
                    writeU32(bodyBytes.size)
                    writeRaw(bodyBytes)
                }
            }
        }

        // Data section (id = 11) omitted for now

        return writer.toByteArray()
    }
}
