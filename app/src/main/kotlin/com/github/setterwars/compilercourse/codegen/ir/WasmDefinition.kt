package com.github.setterwars.compilercourse.codegen.ir

// For simplicity, we are NOT using globals or tables
sealed interface WasmDefinition

enum class WasmValue {
    I32,
    F64,
}

data class FuncType(
    val params: List<WasmValue>,
    val results: List<WasmValue>,
)

data class WasmFunc(
    val type: FuncType,
    val locals: List<WasmValue>,
    val body: List<Instr>,
    val name: String? = null,
    val isStart: Boolean = false,
) : WasmDefinition

interface WasmGlobal : WasmDefinition

data class WasmI32Global(
    val mutable: Boolean = false,
    val initValue: Int,
) : WasmGlobal

data class WasmF64Global(
    val mutable: Boolean = false,
    val initValue: Double,
)

data class WasmMemory(
    val minPages: Int,
    val maxPages: Int? = null,
) : WasmDefinition

enum class ExportKind {
    Func,
    Memory,
}

data class WasmExport(
    val name: String,
    val kind: ExportKind,
    val index: Int,
) : WasmDefinition
