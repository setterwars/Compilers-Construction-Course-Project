package com.github.setterwars.compilercourse.codegen.traverser.common

import com.github.setterwars.compilercourse.codegen.bytecode.ir.ExportKind
import com.github.setterwars.compilercourse.codegen.bytecode.ir.FuncType
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmExport
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmFunc
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmMemory
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmModule
import com.github.setterwars.compilercourse.codegen.traverser.ast.program.resolveProgram
import com.github.setterwars.compilercourse.parser.nodes.Program

private const val FRAMES_OFFSET_PAGES = 10
private const val PAGE_SIZE = (1 shl 10) * 64

class WasmIRGenerator {
    val wasmContext = WasmContext()

    fun generateWasmIr(program: Program): WasmModule {
        val definitions = wasmContext.resolveProgram(program)
        val globals = wasmContext.declarationManager.getGlobals()
        val mainFunc = WasmFunc(
            type = FuncType(emptyList(), emptyList()),
            locals = emptyList(),
            body = buildList {
                addAll(wasmContext.declarationManager.getGlobalsInitializers())
                add(I32Const(MemoryManager.FRAME_L_ADDR))
                add(I32Const(FRAMES_OFFSET_PAGES * PAGE_SIZE))
                add(I32Store())
                add(I32Const(MemoryManager.FRAME_R_ADDR))
                add(I32Const(FRAMES_OFFSET_PAGES * PAGE_SIZE))
                add(I32Store())
                add(I32Const(MemoryManager.MEMORY_POINTER_ADDR))
                add(I32Const(MemoryManager.MEMORY_BEGIN))
                add(I32Store())

            },
            name = null,
            isStart = true,
        )
        val module = WasmModule(
            definitions = buildList {
                addAll(definitions)
                addAll(globals)
                add(WasmMemory(FRAMES_OFFSET_PAGES * 2))
                add(mainFunc)
                add(WasmExport(name = "memory", kind = ExportKind.Memory, index = 0))
                addAll(wasmContext.declarationManager.getRoutinesExports())
            }
        )
        return module
    }
}