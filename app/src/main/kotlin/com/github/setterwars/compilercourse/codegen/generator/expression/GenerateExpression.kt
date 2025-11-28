package com.github.setterwars.compilercourse.codegen.generator.expression

import com.github.setterwars.compilercourse.codegen.generator.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.ir.F64BinOp
import com.github.setterwars.compilercourse.codegen.ir.F64Binary
import com.github.setterwars.compilercourse.codegen.ir.F64Const
import com.github.setterwars.compilercourse.codegen.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.ir.I32Const
import com.github.setterwars.compilercourse.codegen.ir.I32Unary
import com.github.setterwars.compilercourse.codegen.ir.I32UnaryOp
import com.github.setterwars.compilercourse.codegen.ir.Instr
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

fun generateExpression(expression: Expression): ApplyOperatorResult {
    val iss = mutableListOf<Instr>()
    var res = generateRelation(expression.relation)
    for ((op, relation) in expression.rest ?: emptyList()) {
        val res1 = generateRelation(relation)
        iss.addAll(res1.instr)
        res = when (op) {
            ExpressionOperator.AND -> { And.apply(res, res1) }
            ExpressionOperator.OR -> { Or.apply(res, res1) }
            ExpressionOperator.XOR -> { Xor.apply(res, res1) }
        }
        iss.addAll(res.instr)
    }
    return res.copy(instr = iss)
}

fun generateRelation(relation: Relation): ApplyOperatorResult {
    val iss = mutableListOf<Instr>()
    var res = generateSimple(relation.simple)
    if (relation.comparison != null) {
        val res1 = generateSimple(relation.comparison.second)
        iss.addAll(res1.instr)
        res = when (relation.comparison.first) {
            RelationOperator.LT -> { Lt.apply(res, res1) }
            RelationOperator.LE -> { Le.apply(res, res1) }
            RelationOperator.GT -> { Gt.apply(res, res1) }
            RelationOperator.GE -> { Ge.apply(res, res1) }
            RelationOperator.EQ -> { Eq.apply(res, res1) }
            RelationOperator.NEQ -> { Ne.apply(res, res1) }
        }
        iss.addAll(res.instr)
    }
    return res.copy(instr = iss)
}

fun generateSimple(simple: Simple): ApplyOperatorResult {
    val iss = mutableListOf<Instr>()
    var res = generateFactor(simple.factor)
    for ((op, factor) in simple.rest ?: emptyList()) {
        val res1 = generateFactor(factor)
        iss.addAll(res1.instr)
        res = when (op) {
            SimpleOperator.PLUS -> {
                Add.apply(res, res1)
            }

            SimpleOperator.MINUS -> {
                Sub.apply(res, res1)
            }
        }
        iss.addAll(res.instr)
    }
    return res.copy(instr = iss)
}

fun generateFactor(factor: Factor): ApplyOperatorResult {
    val iss = mutableListOf<Instr>()
    var res = generateSummand(factor.summand)
    iss.addAll(res.instr)
    for ((op, summand) in factor.rest ?: emptyList()) {
        val res1 = generateSummand(summand)
        iss.addAll(res1.instr)
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
        iss.addAll(res.instr)
    }
    return res.copy(instr = iss)
}

fun generateSummand(summand: Summand): ApplyOperatorResult {
    return when (summand) {
        is Primary -> generatePrimary(summand)
        is ExpressionInParenthesis -> generateExpression(summand.expression)
    }
}

fun generatePrimary(primary: Primary): ApplyOperatorResult {
    return when (primary) {
        is UnaryInteger -> {
            val iss = mutableListOf<Instr>()
            val int = primary.integerLiteral.getInt()
            return if (primary.unaryOperator is UnaryNot) {
                iss.add(I32Const(int))
                iss.add(I32Unary(I32UnaryOp.EQZ))
                ApplyOperatorResult(
                    onStackValueType = CellValueType.I32Boolean,
                    compileTimeValue = CompileTimeBoolean(int == 0),
                    instr = iss
                )
            } else if (primary.unaryOperator == UnarySign.MINUS) {
                iss.add(I32Const(0))
                iss.add(I32Const(int))
                iss.add(I32Binary(I32BinOp.Sub))
                ApplyOperatorResult(
                    onStackValueType = CellValueType.I32,
                    compileTimeValue = CompileTimeInteger(-int),
                    instr = iss
                )
            } else {
                iss.add(I32Const(int))
                ApplyOperatorResult(
                    onStackValueType = CellValueType.I32,
                    compileTimeValue = CompileTimeInteger(int),
                    instr = iss
                )
            }
        }
        is UnaryReal -> {
            val iss = mutableListOf<Instr>()
            val double = primary.realLiteral.getDouble()
            if (primary.unaryRealOperator == UnarySign.MINUS) {
                iss.add(F64Const(0.0))
                iss.add(F64Const(double))
                iss.add(F64Binary(F64BinOp.Sub))
                ApplyOperatorResult(
                    onStackValueType = CellValueType.F64,
                    compileTimeValue = CompileTimeReal(-double),
                    instr = iss
                )
            } else {
                iss.add(F64Const(double))
                ApplyOperatorResult(
                    onStackValueType = CellValueType.F64,
                    compileTimeValue = CompileTimeReal(double),
                    instr = iss
                )
            }
        }
        is BooleanLiteral -> {
            val iss = mutableListOf<Instr>()
            iss.add(I32Const(if (primary == BooleanLiteral.TRUE) 1 else 0))
            return ApplyOperatorResult(
                onStackValueType = CellValueType.I32Boolean,
                compileTimeValue = CompileTimeBoolean(primary == BooleanLiteral.TRUE),
                instr = iss,
            )
        }
        is UnaryModifiablePrimary -> {
            TODO()
        }
        is RoutineCall -> {
            TODO()
        }
        else -> { TODO() }
    }
}

fun IntegerLiteral.getInt() = token.lexeme.toInt()

fun RealLiteral.getDouble() = token.lexeme.toDouble()