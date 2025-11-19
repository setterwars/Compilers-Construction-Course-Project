package com.github.setterwars.compilercourse.codegen.traverse

import com.github.setterwars.compilercourse.codegen.ir.F64BinOp
import com.github.setterwars.compilercourse.codegen.ir.F64Binary
import com.github.setterwars.compilercourse.codegen.ir.F64Compare
import com.github.setterwars.compilercourse.codegen.ir.F64Const
import com.github.setterwars.compilercourse.codegen.ir.F64RelOp
import com.github.setterwars.compilercourse.codegen.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.ir.I32Compare
import com.github.setterwars.compilercourse.codegen.ir.I32Const
import com.github.setterwars.compilercourse.codegen.ir.I32RelOp
import com.github.setterwars.compilercourse.codegen.ir.I32Unary
import com.github.setterwars.compilercourse.codegen.ir.I32UnaryOp
import com.github.setterwars.compilercourse.codegen.ir.Instr
import com.github.setterwars.compilercourse.parser.nodes.BooleanLiteral
import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.ExpressionOperator
import com.github.setterwars.compilercourse.parser.nodes.Factor
import com.github.setterwars.compilercourse.parser.nodes.FactorOperator
import com.github.setterwars.compilercourse.parser.nodes.IntegerLiteral
import com.github.setterwars.compilercourse.parser.nodes.Primary
import com.github.setterwars.compilercourse.parser.nodes.RealLiteral
import com.github.setterwars.compilercourse.parser.nodes.Relation
import com.github.setterwars.compilercourse.parser.nodes.RelationOperator
import com.github.setterwars.compilercourse.parser.nodes.Simple
import com.github.setterwars.compilercourse.parser.nodes.SimpleOperator
import com.github.setterwars.compilercourse.parser.nodes.Summand
import com.github.setterwars.compilercourse.parser.nodes.UnaryInteger
import com.github.setterwars.compilercourse.parser.nodes.UnaryModifiablePrimary
import com.github.setterwars.compilercourse.parser.nodes.UnaryNot
import com.github.setterwars.compilercourse.parser.nodes.UnaryReal
import com.github.setterwars.compilercourse.parser.nodes.UnarySign

data class GenExpressionResult(
    val instructions: List<Instr>,
    val onStack: StackValue,
)

fun WasmStructureGenerator.genRealLiteral(realLiteral: RealLiteral): List<Instr> {
    return listOf(F64Const(realLiteral.token.lexeme.toDouble()))
}

fun WasmStructureGenerator.genIntegerLiteral(integerLiteral: IntegerLiteral): List<Instr> {
    return listOf(I32Const(integerLiteral.token.lexeme.toInt()))
}

fun WasmStructureGenerator.genBooleanLiteral(booleanLiteral: BooleanLiteral): List<Instr> {
    return listOf(I32Const(if (booleanLiteral == BooleanLiteral.TRUE) 1 else 0))
}

fun WasmStructureGenerator.genUnaryModifiablePrimary(unaryModifiablePrimary: UnaryModifiablePrimary): GenExpressionResult {
    TODO()
}

fun WasmStructureGenerator.genUnaryReal(unaryReal: UnaryReal): List<Instr> {
    val result = mutableListOf<Instr>()
    result.addAll(genRealLiteral(unaryReal.realLiteral))
    if (unaryReal.unaryRealOperator == UnarySign.MINUS) {
        result.add(0, F64Const(0.0))
        result.add(F64Binary(F64BinOp.Sub))
    }
    return result
}

fun WasmStructureGenerator.genUnaryInteger(unaryInteger: UnaryInteger): List<Instr> {
    val result = mutableListOf<Instr>()
    result.addAll(genIntegerLiteral(unaryInteger.integerLiteral))
    if (unaryInteger.unaryOperator == UnarySign.MINUS) {
        result.add(0, I32Const(0))
        result.add(I32Binary(I32BinOp.Sub))
    }
    if (unaryInteger.unaryOperator == UnaryNot) {
        result.add(I32Unary(I32UnaryOp.EQZ))
    }
    return result
}

fun WasmStructureGenerator.genPrimary(primary: Primary): GenExpressionResult {
    return when (primary) {
        is UnaryInteger -> {
            val instructions = genUnaryInteger(primary)
            return GenExpressionResult(
                instructions = instructions,
                onStack = StackValue.I32
            )
        }

        is UnaryReal -> {
            val instructions = genUnaryReal(primary)
            return GenExpressionResult(
                instructions = instructions,
                onStack = StackValue.F64
            )
        }

        is UnaryModifiablePrimary -> {
            return genUnaryModifiablePrimary(primary)
        }

        else -> TODO()
    }
}

fun WasmStructureGenerator.genSummand(summand: Summand): GenExpressionResult {
    TODO()
}

fun WasmStructureGenerator.genFactor(factor: Factor): GenExpressionResult {
    val summandGenResult = genSummand(factor.summand)
    if (factor.rest == null || factor.rest.isEmpty()) {
        return summandGenResult
    }

    val instructions = mutableListOf<Instr>()
    instructions.addAll(summandGenResult.instructions)

    for ((op, next) in factor.rest) {
        val nextGenResult = genSummand(next)
        instructions.addAll(nextGenResult.instructions)
        if (nextGenResult.onStack != summandGenResult.onStack) {
            throw CodegenException()
        }

        when (summandGenResult.onStack) {
            StackValue.I32 -> {
                val binOp = when (op) {
                    FactorOperator.PRODUCT  -> I32BinOp.Mul
                    FactorOperator.DIVISION -> I32BinOp.DivS
                    FactorOperator.MODULO   -> I32BinOp.RemS
                }
                instructions.add(I32Binary(binOp))
            }

            StackValue.F64 -> {
                val binOp = when (op) {
                    FactorOperator.PRODUCT  -> F64BinOp.Mul
                    FactorOperator.DIVISION -> F64BinOp.Div
                    FactorOperator.MODULO   -> throw CodegenException()
                }
                instructions.add(F64Binary(binOp))
            }

            else -> throw CodegenException()
        }

    }
    return GenExpressionResult(
        instructions = instructions,
        onStack = summandGenResult.onStack
    )
}

fun WasmStructureGenerator.genSimple(simple: Simple): GenExpressionResult {
    val factorGenResult = genFactor(simple.factor)
    if (simple.rest == null || simple.rest.isEmpty()) {
        return factorGenResult
    }

    val instructions = mutableListOf<Instr>()
    instructions.addAll(factorGenResult.instructions)
    for ((op, next) in simple.rest) {
        val nextGenResult = genFactor(next)
        instructions.addAll(nextGenResult.instructions)
        if (nextGenResult.onStack != factorGenResult.onStack) {
            throw CodegenException()
        }

        when (factorGenResult.onStack) {
            StackValue.I32 -> {
                val binOp = when (op) {
                    SimpleOperator.PLUS  -> I32BinOp.Add
                    SimpleOperator.MINUS -> I32BinOp.Sub
                }
                instructions.add(I32Binary(binOp))
            }

            StackValue.F64 -> {
                val binOp = when (op) {
                    SimpleOperator.PLUS  -> F64BinOp.Add
                    SimpleOperator.MINUS -> F64BinOp.Sub
                }
                instructions.add(F64Binary(binOp))
            }

            else -> throw CodegenException()
        }
    }
    return GenExpressionResult(
        instructions = instructions,
        onStack = factorGenResult.onStack,
    )
}

fun WasmStructureGenerator.genRelation(relation: Relation): GenExpressionResult {
    val simpleGenResult = genSimple(relation.simple)
    if (relation.comparison == null) {
        return simpleGenResult
    }

    val instructions = mutableListOf<Instr>()
    instructions.addAll(simpleGenResult.instructions)

    val anotherGenResult = genSimple(relation.comparison.second)
    if (anotherGenResult.onStack != simpleGenResult.onStack) {
        throw CodegenException()
    }
    instructions.addAll(anotherGenResult.instructions)

    when (simpleGenResult.onStack) {
        StackValue.I32 -> {
            val op = when (relation.comparison.first) {
                RelationOperator.LT  -> I32RelOp.LtS
                RelationOperator.LE  -> I32RelOp.LeS
                RelationOperator.GT  -> I32RelOp.GtS
                RelationOperator.GE  -> I32RelOp.GeS
                RelationOperator.EQ  -> I32RelOp.Eq
                RelationOperator.NEQ -> I32RelOp.Ne
            }
            instructions.add(I32Compare(op))
        }

        StackValue.F64 -> {
            val op = when (relation.comparison.first) {
                RelationOperator.LT  -> F64RelOp.Lt
                RelationOperator.LE  -> F64RelOp.Le
                RelationOperator.GT  -> F64RelOp.Gt
                RelationOperator.GE  -> F64RelOp.Ge
                RelationOperator.EQ  -> F64RelOp.Eq
                RelationOperator.NEQ -> F64RelOp.Ne
            }
            instructions.add(F64Compare(op))
        }

        else -> CodegenException()
    }
    return GenExpressionResult(
        instructions = instructions,
        onStack = StackValue.I32,
    )
}

fun WasmStructureGenerator.genExpression(expression: Expression): GenExpressionResult {
    val relationGenResult = genRelation(expression.relation)
    if (expression.rest == null || expression.rest.isEmpty()) {
        return relationGenResult
    }
    if (relationGenResult.onStack != StackValue.I32) {
        throw CodegenException()
    }

    val instructions = mutableListOf<Instr>()
    instructions.addAll(relationGenResult.instructions)
    for ((op, next) in expression.rest) {
        val nextGenResult = genRelation(next)
        instructions.addAll(nextGenResult.instructions)
        if (nextGenResult.onStack != relationGenResult.onStack) {
            throw CodegenException()
        }

        val binaryOp = when (op) {
            ExpressionOperator.OR -> I32BinOp.Or
            ExpressionOperator.AND -> I32BinOp.And
            ExpressionOperator.XOR -> I32BinOp.Xor
        }
        instructions.add(I32Binary(binaryOp))
    }
    return GenExpressionResult(
        instructions = instructions,
        onStack = StackValue.I32,
    )
}

