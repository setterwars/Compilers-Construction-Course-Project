package com.github.setterwars.compilercourse.codegen.traverse

import com.github.setterwars.compilercourse.codegen.ir.ExportKind
import com.github.setterwars.compilercourse.codegen.ir.FuncType
import com.github.setterwars.compilercourse.codegen.ir.I32Const
import com.github.setterwars.compilercourse.codegen.ir.I32Load
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.ir.WasmDefinition
import com.github.setterwars.compilercourse.codegen.ir.WasmExport
import com.github.setterwars.compilercourse.codegen.ir.WasmFunc
import com.github.setterwars.compilercourse.codegen.ir.WasmMemory
import com.github.setterwars.compilercourse.codegen.ir.WasmModule
import com.github.setterwars.compilercourse.codegen.traverse.genNode.genRoutineDeclaration
import com.github.setterwars.compilercourse.codegen.traverse.genNode.genSimpleDeclaration
import com.github.setterwars.compilercourse.parser.nodes.Program
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.SimpleDeclaration
import com.github.setterwars.compilercourse.semantic.SemanticInfoStore

class WasmStructureGenerator(val semanticInfoStore: SemanticInfoStore) {

    val memoryManager = MemoryManager() // we have single strip of memory
    val declarationManager = DeclarationManager(memoryManager) // symbol table basically

    val wasmDefinitions = mutableListOf<WasmDefinition>()

    fun generate(program: Program): WasmModule {
        declarationManager.newScope()

        wasmDefinitions.add(WasmMemory(1))
        for (declaration in program.declarations) {
            if (declaration is RoutineDeclaration) {
                wasmDefinitions.add(genRoutineDeclaration(declaration))
            }
            if (declaration is SimpleDeclaration) {
                genSimpleDeclaration(declaration)
            }
        }
        for (routine in declarationManager.routines.values) {
            wasmDefinitions.add(
                WasmExport(
                    name = routine.name,
                    kind = ExportKind.Func,
                    index = routine.orderIndex
                )
            )
        }

        val fillAddressInstructions = mutableListOf<Instr>()
        for (vd in declarationManager.allVariables) {
            if (vd.cellType is CellType.ArrayReference) {
                fillAddressInstructions.add(I32Const(vd.address))
                fillAddressInstructions.add(I32Const(memoryManager.getCurrentPointer()))
                fillAddressInstructions.add(I32Load())
                memoryManager.advance(vd.cellType.memArray.count * vd.cellType.memArray.cellType.bytesSize)
            }
            if (vd.cellType is CellType.RecordReference) {
                fillAddressInstructions.add(I32Const(vd.address))
                fillAddressInstructions.add(I32Const(memoryManager.getCurrentPointer()))
                fillAddressInstructions.add(I32Load())
                memoryManager.advance(vd.cellType.memRecord.fields.sumOf { it.second.bytesSize })
            }
        }
        val mainFunction = WasmFunc(
            type = FuncType(emptyList(), emptyList()),
            locals = emptyList(),
            body = fillAddressInstructions,
            name = "_main",
            isStart = true
        )
        wasmDefinitions.add(mainFunction)
        return WasmModule(definitions = wasmDefinitions)
    }
}