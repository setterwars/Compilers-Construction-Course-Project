package com.github.setterwars.compilercourse.codegen.traverser.ast.expression

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Load
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Unary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32UnaryOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.traverser.ast.modifiablePrimary.resolveModifiablePrimary
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.CodegenException
import com.github.setterwars.compilercourse.parser.nodes.BooleanLiteral
import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.ExpressionInParenthesis
import com.github.setterwars.compilercourse.parser.nodes.ExpressionOperator
import com.github.setterwars.compilercourse.parser.nodes.Factor
import com.github.setterwars.compilercourse.parser.nodes.FactorOperator
import com.github.setterwars.compilercourse.parser.nodes.IntegerLiteral
import com.github.setterwars.compilercourse.parser.nodes.Primary
import com.github.setterwars.compilercourse.parser.nodes.RealLiteral
import com.github.setterwars.compilercourse.parser.nodes.Relation
import com.github.setterwars.compilercourse.parser.nodes.RelationOperator
import com.github.setterwars.compilercourse.parser.nodes.RoutineCall
import com.github.setterwars.compilercourse.parser.nodes.Simple
import com.github.setterwars.compilercourse.parser.nodes.SimpleOperator
import com.github.setterwars.compilercourse.parser.nodes.Summand
import com.github.setterwars.compilercourse.parser.nodes.UnaryInteger
import com.github.setterwars.compilercourse.parser.nodes.UnaryModifiablePrimary
import com.github.setterwars.compilercourse.parser.nodes.UnaryNot
import com.github.setterwars.compilercourse.parser.nodes.UnaryReal
import com.github.setterwars.compilercourse.parser.nodes.UnarySign

data class ExpressionResult(
    val onStackValue: CellValueType,
    val block: Block
)

fun WasmContext.resolveExpression(expression: Expression): ApplyOperatorResult {
    val iss = mutableListOf<Instr>()
    var res = generateRelation(expression.relation)
    iss.addAll(res.instructions)
    for ((op, relation) in expression.rest ?: emptyList()) {
        val res1 = generateRelation(relation)
        iss.addAll(res1.instructions)
        res = when (op) {
            ExpressionOperator.AND -> {
                And.apply(res, res1)
            }

            ExpressionOperator.OR -> {
                Or.apply(res, res1)
            }

            ExpressionOperator.XOR -> {
                Xor.apply(res, res1)
            }
        }
        iss.addAll(res.instructions)
    }
    return res.copy(instructions = iss)
}

fun WasmContext.generateRelation(relation: Relation): ApplyOperatorResult {
    val iss = mutableListOf<Instr>()
    var res = generateSimple(relation.simple)
    iss.addAll(res.instructions)
    if (relation.comparison != null) {
        val res1 = generateSimple(relation.comparison.second)
        iss.addAll(res1.instructions)
        res = when (relation.comparison.first) {
            RelationOperator.LT -> {
                Lt.apply(res, res1)
            }

            RelationOperator.LE -> {
                Le.apply(res, res1)
            }

            RelationOperator.GT -> {
                Gt.apply(res, res1)
            }

            RelationOperator.GE -> {
                Ge.apply(res, res1)
            }

            RelationOperator.EQ -> {
                Eq.apply(res, res1)
            }

            RelationOperator.NEQ -> {
                Ne.apply(res, res1)
            }
        }
        iss.addAll(res.instructions)
    }
    return res.copy(instructions = iss)
}

fun WasmContext.generateSimple(simple: Simple): ApplyOperatorResult {
    val iss = mutableListOf<Instr>()
    var res = generateFactor(simple.factor)
    iss.addAll(res.instructions)
    for ((op, factor) in simple.rest ?: emptyList()) {
        val res1 = generateFactor(factor)
        iss.addAll(res1.instructions)
        res = when (op) {
            SimpleOperator.PLUS -> {
                Add.apply(res, res1)
            }

            SimpleOperator.MINUS -> {
                Sub.apply(res, res1)
            }
        }
        iss.addAll(res.instructions)
    }
    return res.copy(instructions = iss)
}

fun WasmContext.generateFactor(factor: Factor): ApplyOperatorResult {
    val iss = mutableListOf<Instr>()
    var res = generateSummand(factor.summand)
    iss.addAll(res.instructions)
    for ((op, summand) in factor.rest ?: emptyList()) {
        val res1 = generateSummand(summand)
        iss.addAll(res1.instructions)
        res = when (op) {
            FactorOperator.PRODUCT -> {
                Mul.apply(res, res1)
            }

            FactorOperator.DIVISION -> {
                Div.apply(res, res1)
            }

            FactorOperator.MODULO -> {
                Mod.apply(res, res1)
            }
        }
        iss.addAll(res.instructions)
    }
    return res.copy(instructions = iss)
}

fun WasmContext.generateSummand(summand: Summand): ApplyOperatorResult {
    return when (summand) {
        is Primary -> generatePrimary(summand)
        is ExpressionInParenthesis -> resolveExpression(summand.expression)
    }
}

fun WasmContext.generatePrimary(primary: Primary): ApplyOperatorResult {
    return when (primary) {
        is UnaryInteger -> {
            return ApplyOperatorResult(
                onStackValueType = CellValueType.I32,
                instructions = listOf(
                    I32Load(primary.data?.value ?: throw CodegenException())
                )
            )
        }

        is UnaryReal -> {
            return ApplyOperatorResult(
                onStackValueType = CellValueType.F64,
                instructions = listOf(
                    F64Const(primary.data?.value ?: throw CodegenException())
                )
            )
        }

        is BooleanLiteral -> {
            val iss = mutableListOf<Instr>()
            iss.add(I32Const(if (primary == BooleanLiteral.TRUE) 1 else 0))
            return ApplyOperatorResult(
                onStackValueType = CellValueType.I32Boolean,
                instructions = iss,
            )
        }

        is UnaryModifiablePrimary -> {
            val result = resolveModifiablePrimary(primary.modifiablePrimary)
            val iss = when (primary.unaryOperator) {
                is UnaryNot -> buildList {
                    if (result.cellValueType == CellValueType.I32 || result.cellValueType == CellValueType.I32Boolean) {
                        addAll(result.instructions)
                        add(I32Unary(I32UnaryOp.EQZ))
                    } else {
                        throw CodegenException()
                    }
                }

                UnarySign.MINUS -> buildList {
                    if (result.cellValueType == CellValueType.F64) {
                        add(F64Const(0.0))
                        addAll(result.instructions)
                        add(F64Binary(F64BinOp.Sub))
                    } else if (
                        result.cellValueType == CellValueType.I32
                    ) {
                        add(I32Const(0))
                        addAll(result.instructions)
                        add(I32Binary(I32BinOp.Sub))
                    } else {
                        throw CodegenException()
                    }
                }

                UnarySign.PLUS -> {
                    if (result.cellValueType == CellValueType.I32Boolean || result.cellValueType is CellValueType.MemoryReference) {
                        result.instructions
                    } else {
                        throw CodegenException()
                    }
                }

                else -> result.instructions
            }
            val onStackValueType = when (primary.unaryOperator) {
                is UnaryNot -> CellValueType.I32Boolean
                else -> result.cellValueType
            }
            return ApplyOperatorResult(
                onStackValueType = onStackValueType,
                instructions = iss
            )
            TODO()
        }

        is RoutineCall -> {
            TODO()
        }

        else -> {
            TODO()
        }
    }
}

fun IntegerLiteral.getInt() = token.lexeme.toInt()

fun RealLiteral.getDouble() = token.lexeme.toDouble()