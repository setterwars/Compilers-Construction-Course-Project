package com.github.setterwars.compilercourse.codegen.traverse

import com.github.setterwars.compilercourse.codegen.ir.Call
import com.github.setterwars.compilercourse.codegen.ir.ExportKind
import com.github.setterwars.compilercourse.codegen.ir.F64Store
import com.github.setterwars.compilercourse.codegen.ir.FuncType
import com.github.setterwars.compilercourse.codegen.ir.I32Const
import com.github.setterwars.compilercourse.codegen.ir.I32Load
import com.github.setterwars.compilercourse.codegen.ir.I32Store
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.ir.LocalGet
import com.github.setterwars.compilercourse.codegen.ir.ValueType
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
        for ((idx, routine) in declarationManager.routines.values.withIndex()) {
            val params = mutableListOf<ValueType>()
            val instructions = mutableListOf<Instr>()
            for ((paramIdx, param) in routine.parameters.withIndex()) {
                when (param.cellType) {
                    is CellType.I32 -> params.add(ValueType.I32)
                    is CellType.F64 -> params.add(ValueType.F64)
                    else -> params.add(ValueType.I32)
                }

                instructions.add(I32Const(param.address))
                instructions.add(LocalGet(paramIdx))
                when (params.last()) {
                    ValueType.I32 -> instructions.add(I32Store())
                    ValueType.F64 -> instructions.add(F64Store())
                }
            }
            instructions.add(Call(routine.orderIndex))
            wasmDefinitions.add(
                WasmFunc(
                    type = FuncType(params = params, results = emptyList()),
                    locals = emptyList(),
                    body = instructions,
                    name = "_${routine.name}_launcher",
                )
            )
            wasmDefinitions.add(
                WasmExport(
                    name = routine.name,
                    kind = ExportKind.Func,
                    index = declarationManager.routines.size + idx
                )
            )
        }

        val fillAddressInstructions = mutableListOf<Instr>()
        for (vd in declarationManager.allVariables) {
            if (vd.cellType is CellType.ArrayReference) {
                fillAddressInstructions.add(I32Const(vd.address))
                fillAddressInstructions.add(I32Const(memoryManager.getCurrentPointer()))
                fillAddressInstructions.add(I32Store())
                memoryManager.advance(vd.cellType.memArray.count * vd.cellType.memArray.cellType.bytesSize)
            }
            if (vd.cellType is CellType.RecordReference) {
                fillAddressInstructions.add(I32Const(vd.address))
                fillAddressInstructions.add(I32Const(memoryManager.getCurrentPointer()))
                fillAddressInstructions.add(I32Store())
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
        wasmDefinitions.add(WasmExport("memory", ExportKind.Memory, 0))
        return WasmModule(definitions = wasmDefinitions)
    }
}