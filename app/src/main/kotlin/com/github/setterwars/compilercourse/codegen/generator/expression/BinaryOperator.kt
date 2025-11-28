package com.github.setterwars.compilercourse.codegen.generator.expression

import com.github.setterwars.compilercourse.codegen.generator.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.ir.F64BinOp
import com.github.setterwars.compilercourse.codegen.ir.F64Binary
import com.github.setterwars.compilercourse.codegen.ir.F64Compare
import com.github.setterwars.compilercourse.codegen.ir.F64RelOp
import com.github.setterwars.compilercourse.codegen.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.ir.I32Compare
import com.github.setterwars.compilercourse.codegen.ir.I32RelOp
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.codegen.ir.Nop

data class ApplyOperatorResult(
    val onStackValueType: CellValueType,
    val compileTimeValue: CompileTimeValue?,
    val instr: List<Instr>,
)

interface BinaryOperator {

    fun apply(op1: ApplyOperatorResult, op2: ApplyOperatorResult): ApplyOperatorResult
}

fun createOperator(
    allowedCellTypes: List<CellValueType>,
    onI32: () -> List<Instr>, // when two top values in stack are signed I32
    onF64: () -> List<Instr>, // when two top values in stack are F64,
    onCompileTimeValue: (CompileTimeValue?, CompileTimeValue?) -> CompileTimeValue?,
): BinaryOperator{
    return object : BinaryOperator {
        override fun apply(op1: ApplyOperatorResult, op2: ApplyOperatorResult): Pair<List<Instr>, ApplyOperatorResult> {
            TODO()
        }
    }
}

fun createOperator(
    allowedCellTypes: List<CellValueType>? = null,
    onI32: Instr? = null, // when two top values in stack are signed I32
    onF64: Instr? = null, // when two top values in stack are F64,
    onCompileTimeReal: ((Double, Double) -> CompileTimeValue?)? = null,
    onCompileTimeInteger: ((Int, Int) -> CompileTimeValue?)? = null,
    onCompileTimeBoolean: ((Boolean, Boolean) -> CompileTimeValue?)? = null,
): BinaryOperator {
    return object : BinaryOperator {
        override fun apply(op1: ApplyOperatorResult, op2: ApplyOperatorResult): Pair<List<Instr>, ApplyOperatorResult> {
            TODO()
        }
    }
}

fun Boolean.toInteger() = this.compareTo(false)

val And = createOperator(
    allowedCellTypes = listOf(CellValueType.I32Boolean),
    onI32 = I32Binary(I32BinOp.And),
    onF64 = Nop,
    onCompileTimeBoolean = { a: Boolean, b: Boolean -> CompileTimeBoolean(a && b) }
)

val Or = createOperator(
    allowedCellTypes = listOf(CellValueType.I32Boolean),
    onI32 = I32Binary(I32BinOp.Or),
    onF64 = Nop,
    onCompileTimeBoolean = { a: Boolean, b: Boolean -> CompileTimeBoolean(a || b) }
)

val Xor = createOperator(
    allowedCellTypes = listOf(CellValueType.I32Boolean),
    onI32 = I32Binary(I32BinOp.Xor),
    onF64 = Nop,
    onCompileTimeBoolean = { a: Boolean, b: Boolean -> CompileTimeBoolean(a xor b) }
)

val Lt = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.LtS),
    onF64 = F64Compare(F64RelOp.Lt),
    onCompileTimeReal = { a, b -> CompileTimeBoolean(a < b) },
    onCompileTimeInteger = { a, b -> CompileTimeBoolean(a < b) },
    onCompileTimeBoolean = { a, b -> CompileTimeBoolean(a < b) }
)


val Le = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.LeS),
    onF64 = F64Compare(F64RelOp.Le),
    onCompileTimeReal = { a, b -> CompileTimeBoolean(a <= b) },
    onCompileTimeInteger = { a, b -> CompileTimeBoolean(a <= b) },
    onCompileTimeBoolean = { a, b -> CompileTimeBoolean(a <= b) }
)


val Gt = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.GtS),
    onF64 = F64Compare(F64RelOp.Gt),
    onCompileTimeReal = { a, b -> CompileTimeBoolean(a > b) },
    onCompileTimeInteger = { a, b -> CompileTimeBoolean(a > b) },
    onCompileTimeBoolean = { a, b -> CompileTimeBoolean(a > b) }
)


val Ge = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.GeS),
    onF64 = F64Compare(F64RelOp.Ge),
    onCompileTimeReal = { a, b -> CompileTimeBoolean(a >= b) },
    onCompileTimeInteger = { a, b -> CompileTimeBoolean(a >= b) },
    onCompileTimeBoolean = { a, b -> CompileTimeBoolean(a >= b) }
)

val Eq = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.Eq),
    onF64 = F64Compare(F64RelOp.Eq),
    onCompileTimeReal = { a, b -> CompileTimeBoolean(a == b) },
    onCompileTimeInteger = { a, b -> CompileTimeBoolean(a == b) },
    onCompileTimeBoolean = { a, b -> CompileTimeBoolean(a == b) }
)

val Ne = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64, CellValueType.I32Boolean),
    onI32 = I32Compare(I32RelOp.Ne),
    onF64 = F64Compare(F64RelOp.Ne),
    onCompileTimeReal = { a, b -> CompileTimeBoolean(a != b) },
    onCompileTimeInteger = { a, b -> CompileTimeBoolean(a != b) },
    onCompileTimeBoolean = { a, b -> CompileTimeBoolean(a != b) }
)

val Add = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.I32Boolean, CellValueType.F64),
    onI32 = I32Binary(I32BinOp.Add),
    onF64 = F64Binary(F64BinOp.Add),
    onCompileTimeReal = { a, b -> CompileTimeReal(a + b) },
    onCompileTimeInteger = { a, b -> CompileTimeInteger(a + b) },
    onCompileTimeBoolean = { a, b -> CompileTimeInteger(a.toInteger() + b.toInteger()) }
)

val Sub = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64),
    onI32 = I32Binary(I32BinOp.Sub),
    onF64 = F64Binary(F64BinOp.Sub),
    onCompileTimeReal = { a, b -> CompileTimeReal(a - b) },
    onCompileTimeInteger = { a, b -> CompileTimeInteger(a - b) },
)

val Mul = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64),
    onI32 = I32Binary(I32BinOp.Mul),
    onF64 = F64Binary(F64BinOp.Mul),
    onCompileTimeReal = { a, b -> CompileTimeReal(a * b ) },
    onCompileTimeInteger = { a, b -> CompileTimeInteger(a + b ) },
)

val Div = createOperator(
    allowedCellTypes = listOf(CellValueType.I32, CellValueType.F64),
    onI32 = I32Binary(I32BinOp.DivS),
    onF64 = F64Binary(F64BinOp.Div),
    onCompileTimeReal = { a, b -> CompileTimeReal(a / b ) },
    onCompileTimeInteger = { a, b -> CompileTimeInteger(a / b ) },
)

val Mod = createOperator(
    allowedCellTypes = listOf(CellValueType.I32),
    onI32 = I32Binary(I32BinOp.RemS),
    onCompileTimeInteger = { a, b -> CompileTimeInteger(a % b ) },
)



