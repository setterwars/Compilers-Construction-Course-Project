package com.github.setterwars.compilercourse.codegen.ir

sealed interface Instr

// Control
object Unreachable : Instr
object Nop : Instr

data class Block(
    val resultType: ValueType?, // null = no result
    val instructions: List<Instr>,
) : Instr

data class Loop(
    val resultType: ValueType?,
    val instructions: List<Instr>,
) : Instr

data class If(
    val resultType: ValueType?,
    val thenInstrs: List<Instr>,
    val elseInstrs: List<Instr> = emptyList(),
) : Instr

data class Br(val depth: Int) : Instr // label depth

data class BrIf(val depth: Int) : Instr

object Return : Instr

object Drop : Instr


// Call
data class Call(val funcIndex: Int) : Instr


// Locals
data class LocalGet(val index: Int) : Instr
data class LocalSet(val index: Int) : Instr
data class LocalTee(val index: Int) : Instr


// Constants
data class I32Const(val value: Int) : Instr
data class F64Const(val value: Double) : Instr

// Numeric operations
enum class I32BinOp {
    Add, Sub, Mul, DivS, DivU, RemS, RemU, And, Or, Xor
}
data class I32Binary(val op: I32BinOp) : Instr

enum class I32UnaryOp {
    EQZ
}
data class I32Unary(val op: I32UnaryOp) : Instr

enum class F64BinOp {
    Add, Sub, Mul, Div
}
data class F64Binary(val op: F64BinOp) : Instr

enum class I32RelOp {
    Eq, Ne, LtS, LtU, GtS, GtU, LeS, LeU, GeS, GeU,
}
data class I32Compare(val op: I32RelOp) : Instr

enum class F64RelOp {
    Eq, Ne, Lt, Gt, Le, Ge,
}
data class F64Compare(val op: F64RelOp) : Instr

// Memory
data class I32Load(
    val align: Int = 2,
    val offset: Int = 0,
) : Instr

data class I32Store(
    val align: Int = 2,
    val offset: Int = 0,
) : Instr

data class F64Load(
    val align: Int = 3,
    val offset: Int = 0,
) : Instr

data class F64Store(
    val align: Int = 3,
    val offset: Int = 0,
) : Instr

