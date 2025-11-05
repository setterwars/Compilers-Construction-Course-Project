package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.parser.nodes.*

class PrettyAstPrinter(private val info: SemanticInfoStore) {
    private fun ts(type: ResolvedType?): String = when (type) {
        null -> "?"
        ResolvedType.Integer -> "integer"
        ResolvedType.Real -> "real"
        ResolvedType.Boolean -> "boolean"
        ResolvedType.Unknown -> "unknown"
        is ResolvedType.Array -> "array[${type.size?.toString() ?: ""}] ${ts(type.elementType)}"
        is ResolvedType.Record -> "record"
        is ResolvedType.Placeholder -> type.name
    }
    fun print(program: Program): String {
        val sb = StringBuilder()
        sb.appendLine("Program")
        program.declarations.forEach { decl ->
            printDeclaration(sb, decl, 1)
        }
        return sb.toString()
    }

    private fun indent(sb: StringBuilder, level: Int) {
        repeat(level) { sb.append("  ") }
    }

    private fun printDeclaration(sb: StringBuilder, decl: Declaration, level: Int) {
        indent(sb, level)
        when (decl) {
            is VariableDeclarationWithType -> {
                sb.appendLine("Var ${decl.identifier.token.lexeme}: ${decl.type}")
                decl.initialValue?.let {
                    printExpression(sb, it, level + 1)
                }
            }
            is VariableDeclarationNoType -> {
                sb.appendLine("Var ${decl.identifier.token.lexeme}")
                printExpression(sb, decl.initialValue, level + 1)
            }
            is TypeDeclaration -> {
                sb.appendLine("Type ${decl.identifier.token.lexeme} = ${decl.type}")
            }
            is RoutineDeclaration -> {
                sb.appendLine("Routine ${decl.header.name.token.lexeme}")
                when (val body = decl.body) {
                    is FullRoutineBody -> printBody(sb, body.body, level + 1)
                    is SingleExpressionBody -> printExpression(sb, body.expression, level + 1)
                    null -> {
                        indent(sb, level + 1)
                        sb.appendLine("<forward>")
                    }
                }
            }
        }
    }

    private fun printBody(sb: StringBuilder, body: Body, level: Int) {
        body.bodyElements.forEach { el ->
            when (el) {
                is SimpleDeclaration -> printDeclaration(sb, el, level)
                is Statement -> printStatement(sb, el, level)
            }
        }
    }

    private fun printStatement(sb: StringBuilder, st: Statement, level: Int) {
        indent(sb, level)
        when (st) {
            is Assignment -> {
                sb.appendLine("Assignment")
                indent(sb, level + 1)
                sb.appendLine("Target ${st.modifiablePrimary.variable.token.lexeme} : ${ts(info.get<ModifiablePrimarySemanticInfo>(st.modifiablePrimary)?.type)}")
                printExpression(sb, st.expression, level + 1)
            }
            is RoutineCall -> {
                sb.appendLine("Call ${st.routineName.token.lexeme} : ${ts(info.get<RoutineCallSemanticInfo>(st)?.type)}")
                st.arguments.forEach { printExpression(sb, it.expression, level + 1) }
            }
            is WhileLoop -> {
                sb.appendLine("While")
                printExpression(sb, st.condition, level + 1)
                printBody(sb, st.body, level + 1)
            }
            is ForLoop -> {
                sb.appendLine("For ${st.loopVariable.token.lexeme}${if (st.reverse) " downto" else " to"}")
                printExpression(sb, st.range.begin, level + 1)
                st.range.end?.let { printExpression(sb, it, level + 1) }
                printBody(sb, st.body, level + 1)
            }
            is IfStatement -> {
                sb.appendLine("If")
                printExpression(sb, st.condition, level + 1)
                printBody(sb, st.thenBody, level + 1)
                st.elseBody?.let { printBody(sb, it, level + 1) }
            }
            is PrintStatement -> {
                sb.appendLine("Print")
                printExpression(sb, st.expression, level + 1)
                st.rest.forEach { printExpression(sb, it, level + 1) }
            }
        }
    }

    private fun printExpression(sb: StringBuilder, expr: Expression, level: Int) {
        val t = info.get<ExpressionSemanticInfo>(expr)?.type
        indent(sb, level)
        sb.appendLine("Expression : ${ts(t)}")
        printRelation(sb, expr.relation, level + 1)
        expr.rest?.forEach { (op, rel) ->
            indent(sb, level + 1)
            sb.appendLine("${op.name.lowercase()}")
            printRelation(sb, rel, level + 2)
        }
    }

    private fun printRelation(sb: StringBuilder, rel: Relation, level: Int) {
        val inf = info.get<RelationSemanticInfo>(rel)
        indent(sb, level)
        sb.appendLine("Relation : ${ts(inf?.type)} ${inf?.compileTimeValue?.let { "[ct=$it]" } ?: ""}")
        printSimple(sb, rel.simple, level + 1)
        rel.comparison?.let { (op, simple) ->
            indent(sb, level + 1)
            sb.appendLine(op.name)
            printSimple(sb, simple, level + 2)
        }
    }

    private fun printSimple(sb: StringBuilder, simple: Simple, level: Int) {
        val t = info.get<SimpleSemanticInfo>(simple)?.type
        indent(sb, level)
        sb.appendLine("Simple : ${ts(t)}")
        printFactor(sb, simple.factor, level + 1)
        simple.rest?.forEach { (op, factor) ->
            indent(sb, level + 1)
            sb.appendLine(op.name)
            printFactor(sb, factor, level + 2)
        }
    }

    private fun printFactor(sb: StringBuilder, factor: Factor, level: Int) {
        val t = info.get<FactorSemanticInfo>(factor)?.type
        indent(sb, level)
        sb.appendLine("Factor : ${ts(t)}")
        printSummand(sb, factor.summand, level + 1)
        factor.rest?.forEach { (op, summand) ->
            indent(sb, level + 1)
            sb.appendLine(op.name)
            printSummand(sb, summand, level + 2)
        }
    }

    private fun printSummand(sb: StringBuilder, summand: Summand, level: Int) {
        when (summand) {
            is ExpressionInParenthesis -> printExpression(sb, summand.expression, level)
            is Primary -> printPrimary(sb, summand, level)
            else -> {
                indent(sb, level)
                sb.appendLine("<unknown-summand>")
            }
        }
    }

    private fun printPrimary(sb: StringBuilder, primary: Primary, level: Int) {
        when (primary) {
            is UnaryInteger -> {
                val v = info.get<UnaryIntegerSemanticInfo>(primary)?.value
                indent(sb, level)
                sb.appendLine("UnaryInteger ${v ?: "?"}")
            }
            is UnaryReal -> {
                val lit = info.get<RealLiteralSemanticInfo>(primary.realLiteral)?.value
                indent(sb, level)
                sb.appendLine("UnaryReal ${lit ?: "?"}")
            }
            is UnaryModifiablePrimary -> {
                indent(sb, level)
                sb.appendLine("UnaryModifiable")
                val t = info.get<ModifiablePrimarySemanticInfo>(primary.modifiablePrimary)?.type
                indent(sb, level + 1)
                sb.appendLine("ModPrimary : ${ts(t)}")
            }
            is BooleanLiteral -> {
                indent(sb, level)
                sb.appendLine("Boolean ${primary.name.lowercase()}")
            }
            is RoutineCall -> {
                val t = info.get<RoutineCallSemanticInfo>(primary)?.type
                indent(sb, level)
                sb.appendLine("Call ${primary.routineName.token.lexeme} : ${ts(t)}")
                primary.arguments.forEach { printExpression(sb, it.expression, level + 1) }
            }
        }
    }
}


