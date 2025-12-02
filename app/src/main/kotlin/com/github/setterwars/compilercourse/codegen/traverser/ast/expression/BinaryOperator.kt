package com.github.setterwars.compilercourse.codegen.traverser.ast.expression

import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Compare
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64RelOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.GlobalGet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.GlobalSet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Compare
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32RelOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32ToF64S
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.GlobalVariablesManager
import com.github.setterwars.compilercourse.codegen.traverser.common.MemoryManager
import com.github.setterwars.compilercourse.codegen.utils.CodegenException

data class ApplyOperatorResult(
    val onStackValueType: CellValueType,
    val instructions: List<Instr>,
)

interface BinaryOperator {

    fun apply(op1: ApplyOperatorResult, op2: ApplyOperatorResult): ApplyOperatorResult
}

// - If the top two values on the stack are different wasm values, then convert everything to F64
// - Apply the passed instructions
// - The resulting type is decided to the "broadest" type:
//   I32Boolean < I32 = MemoryReference < F64
fun createOperator(
    allowedCellTypes: List<CellValueType>,
    onI32: Instr? = null, // when two top values in stack are signed I32
    onF64: Instr? = null, // when two top values in stack are F64,
    forceReturnResult: CellValueType? = null,
): BinaryOperator {
    return object : BinaryOperator {
        override fun apply(op1: ApplyOperatorResult, op2: ApplyOperatorResult): ApplyOperatorResult {
            val iss = mutableListOf<Instr>()

            val op1Type = op1.onStackValueType
            val op2Type = op2.onStackValueType
            if (op1Type !in allowedCellTypes || op2Type !in allowedCellTypes) {
                throw CodegenException()
            }
            if (op1Type.toWasmValue() == WasmValue.F64 && op2Type.toWasmValue() == WasmValue.I32) {
                iss.add(I32ToF64S) // convert top value on the stack from i32 to f64
            } else if (op1Type.toWasmValue() == WasmValue.I32 && op2Type.toWasmValue() == WasmValue.F64) {
                iss.add(GlobalSet(GlobalVariablesManager.ReservedGlobals.F64.ordinal))
                iss.add(I32ToF64S)
                iss.add(GlobalGet(GlobalVariablesManager.ReservedGlobals.F64.ordinal))
            }
            if (op1Type.toWasmValue() == WasmValue.F64 || op2Type.toWasmValue() == WasmValue.F64) {
                iss.add(onF64!!)
            } else {
                iss.add(onI32!!)
            }
            val resultCellValueType = forceReturnResult
                ?: if (op1Type == CellValueType.F64 || op2Type == CellValueType.F64) {
                    CellValueType.F64
                } else if (
                    (op1Type == CellValueType.I32 || op1Type is CellValueType.MemoryReference) ||
                    (op2Type == CellValueType.I32 || op2Type is CellValueType.MemoryReference)
                ) {
                    CellValueType.I32
                } else {
                    CellValueType.I32Boolean
                }
            return ApplyOperatorResult(
                onStackValueType = resultCellValueType,
                instructions = iss
            )
        }
    }
}

val And = createOperator(
    allowedCellTypes = listOf(CellValueType.I32Boolean),
    onI32 = I32Binary(I32BinOp.And),
    forceReturnResult = CellValueType.I32Boolean,
)

val Or = createOperator(
    allowedCellTypes = listOf(CellValueType.I32Boolean),
    onI32 = I32Binary(I32BinOp.Or),
    forceReturnResult = CellValueType.I32Boolean,
)

val Xor = createOperator(
    allowedCellTypes = listOf(CellValueType.I32Boolean),
    onI32 = I32Binary(I32BinOp.Xor),
    forceReturnResult = CellValueType.I32Boolean,
)

val Lt = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.LtS),
    onF64 = F64Compare(F64RelOp.Lt),
    forceReturnResult = CellValueType.I32Boolean,
)

val Le = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.LeS),
    onF64 = F64Compare(F64RelOp.Le),
    forceReturnResult = CellValueType.I32Boolean,
)

val Gt = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.GtS),
    onF64 = F64Compare(F64RelOp.Gt),
    forceReturnResult = CellValueType.I32Boolean,
)

val Ge = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.GeS),
    onF64 = F64Compare(F64RelOp.Ge),
    forceReturnResult = CellValueType.I32Boolean,
)

val Eq = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.Eq),
    onF64 = F64Compare(F64RelOp.Eq),
    forceReturnResult = CellValueType.I32Boolean,
)

val Ne = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.Ne),
    onF64 = F64Compare(F64RelOp.Ne),
    forceReturnResult = CellValueType.I32Boolean,
)

val Add = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.I32Boolean, CellValueType.F64),
    onI32 = I32Binary(I32BinOp.Add),
    onF64 = F64Binary(F64BinOp.Add),
)

val Sub = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64),
    onI32 = I32Binary(I32BinOp.Sub),
    onF64 = F64Binary(F64BinOp.Sub),
)

val Mul = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64),
    onI32 = I32Binary(I32BinOp.Mul),
    onF64 = F64Binary(F64BinOp.Mul),
)

val Div = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64),
    onI32 = I32Binary(I32BinOp.DivS),
    onF64 = F64Binary(F64BinOp.Div),
)

val Mod = createOperator(
    allowedCellTypes = listOf(CellValueType.I32),
    onI32 = I32Binary(I32BinOp.RemS),
)



