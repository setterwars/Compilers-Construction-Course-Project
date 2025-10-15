package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.lexer.Token

// Core API
sealed interface Symbol

abstract class NonTerminal(open val children: List<Symbol>) : Symbol
abstract class Terminal(open val token: Token) : Symbol

// Terminal Nodes; Naming: T[name]
data class TIdentifier(override val token: Token) : Terminal(token)
data class TIntLiteral(override val token: Token) : Terminal(token)
data class TRealLiteral(override val token: Token) : Terminal(token)
data class TFalse(override val token: Token) : Terminal(token)
data class TTrue(override val token: Token) : Terminal(token)

data class TColon(override val token: Token) : Terminal(token)        // :
data class TLBracket(override val token: Token) : Terminal(token)     // [
data class TRBracket(override val token: Token) : Terminal(token)     // ]
data class TAssign(override val token: Token) : Terminal(token)       // :=
data class TComma(override val token: Token) : Terminal(token)        // ,
data class TLParen(override val token: Token) : Terminal(token)       // (
data class TRParen(override val token: Token) : Terminal(token)       // )
data class TRangeDots(override val token: Token) : Terminal(token)    // ..
data class TArrow(override val token: Token) : Terminal(token)        // =>
data class TSemicolon(override val token: Token) : Terminal(token)    // ;

data class TAnd(override val token: Token) : Terminal(token)
data class TOr(override val token: Token) : Terminal(token)
data class TXor(override val token: Token) : Terminal(token)

data class TLt(override val token: Token) : Terminal(token)
data class TLe(override val token: Token) : Terminal(token)
data class TGt(override val token: Token) : Terminal(token)
data class TGe(override val token: Token) : Terminal(token)
data class TEq(override val token: Token) : Terminal(token)
data class TNe(override val token: Token) : Terminal(token)

data class TPlus(override val token: Token) : Terminal(token)
data class TMinus(override val token: Token) : Terminal(token)
data class TStar(override val token: Token) : Terminal(token)
data class TSlash(override val token: Token) : Terminal(token)
data class TPercent(override val token: Token) : Terminal(token)
data class TDot(override val token: Token) : Terminal(token)

data class TVar(override val token: Token) : Terminal(token)
data class TIs(override val token: Token) : Terminal(token)
data class TTypeKw(override val token: Token) : Terminal(token)
data class TIntegerKw(override val token: Token) : Terminal(token)
data class TRealKw(override val token: Token) : Terminal(token)
data class TBooleanKw(override val token: Token) : Terminal(token)
data class TRecord(override val token: Token) : Terminal(token)
data class TEnd(override val token: Token) : Terminal(token)
data class TArray(override val token: Token) : Terminal(token)
data class TWhile(override val token: Token) : Terminal(token)
data class TLoop(override val token: Token) : Terminal(token)
data class TFor(override val token: Token) : Terminal(token)
data class TIn(override val token: Token) : Terminal(token)
data class TReverse(override val token: Token) : Terminal(token)
data class TIf(override val token: Token) : Terminal(token)
data class TThen(override val token: Token) : Terminal(token)
data class TElse(override val token: Token) : Terminal(token)
data class TPrint(override val token: Token) : Terminal(token)
data class TRoutine(override val token: Token) : Terminal(token)
data class TNot(override val token: Token) : Terminal(token)

data class TNewLine(override val token: Token) : Terminal(token) // layout
data class TEof(override val token: Token) : Terminal(token)

// Non-Terminal Nodes
data class ProgramNode(override val children: List<Symbol>) : NonTerminal(children)

data class SimpleDeclarationNode(override val children: List<Symbol>) : NonTerminal(children)
data class VariableDeclarationNode(override val children: List<Symbol>) : NonTerminal(children)
data class TypeDeclarationNode(override val children: List<Symbol>) : NonTerminal(children)

data class TypeNode(override val children: List<Symbol>) : NonTerminal(children)
data class PrimitiveTypeNode(override val children: List<Symbol>) : NonTerminal(children)
data class UserTypeNode(override val children: List<Symbol>) : NonTerminal(children)
data class RecordTypeNode(override val children: List<Symbol>) : NonTerminal(children)
data class ArrayTypeNode(override val children: List<Symbol>) : NonTerminal(children)

data class StatementNode(override val children: List<Symbol>) : NonTerminal(children)
data class AssignmentNode(override val children: List<Symbol>) : NonTerminal(children)
data class RoutineCallNode(override val children: List<Symbol>) : NonTerminal(children)
data class WhileLoopNode(override val children: List<Symbol>) : NonTerminal(children)
data class ForLoopNode(override val children: List<Symbol>) : NonTerminal(children)
data class RangeNode(override val children: List<Symbol>) : NonTerminal(children)
data class IfStatementNode(override val children: List<Symbol>) : NonTerminal(children)
data class PrintStatementNode(override val children: List<Symbol>) : NonTerminal(children)

data class RoutineDeclarationNode(override val children: List<Symbol>) : NonTerminal(children)
data class RoutineHeaderNode(override val children: List<Symbol>) : NonTerminal(children)
data class RoutineBodyNode(override val children: List<Symbol>) : NonTerminal(children)
data class ParametersNode(override val children: List<Symbol>) : NonTerminal(children)
data class ParameterDeclarationNode(override val children: List<Symbol>) : NonTerminal(children)

data class BodyNode(override val children: List<Symbol>) : NonTerminal(children)

data class ExpressionNode(override val children: List<Symbol>) : NonTerminal(children)
data class RelationNode(override val children: List<Symbol>) : NonTerminal(children)
data class SimpleNode(override val children: List<Symbol>) : NonTerminal(children)  // +/-
data class FactorNode(override val children: List<Symbol>) : NonTerminal(children)  // */%
data class SummandNode(override val children: List<Symbol>) : NonTerminal(children) // Primary | (Expr)
data class PrimaryNode(override val children: List<Symbol>) : NonTerminal(children)
data class SignNode(override val children: List<Symbol>) : NonTerminal(children)
data class ModifiablePrimaryNode(override val children: List<Symbol>) : NonTerminal(children)