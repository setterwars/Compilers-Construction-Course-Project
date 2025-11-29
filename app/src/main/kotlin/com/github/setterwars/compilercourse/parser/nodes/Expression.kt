package com.github.setterwars.compilercourse.parser.nodes

import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.semantic.semanticData.ExpressionSemanticData
import com.github.setterwars.compilercourse.semantic.semanticData.ModifiablePrimarySemanticData
import com.github.setterwars.compilercourse.semantic.semanticData.UnaryIntegerSemanticData
import com.github.setterwars.compilercourse.semantic.semanticData.UnaryRealSemanticData

data class Expression(
    val relation: Relation,
    val rest: List<Pair<ExpressionOperator, Relation>>?,
) : SemanticDataHolder<ExpressionSemanticData>()
enum class ExpressionOperator { AND, OR, XOR }

data class Relation(
    val simple: Simple,
    val comparison: Pair<RelationOperator, Simple>?,
)
enum class RelationOperator { LT, LE, GT, GE, EQ, NEQ }

data class Simple(
    val factor: Factor,
    val rest: List<Pair<SimpleOperator, Factor>>?,
)
enum class SimpleOperator { PLUS, MINUS }

data class Factor(
    val summand: Summand,
    val rest: List<Pair<FactorOperator, Summand>>?,
)
enum class FactorOperator { PRODUCT, DIVISION, MODULO }

sealed interface Summand

data class ExpressionInParenthesis(
    val expression: Expression,
) : Summand

sealed interface Primary : Summand

data class UnaryInteger(
    val unaryOperator: UnaryOperator?,
    val integerLiteral: IntegerLiteral,
) : Primary, SemanticDataHolder<UnaryIntegerSemanticData>()

data class UnaryReal(
    val unaryRealOperator: UnaryRealOperator?,
    val realLiteral: RealLiteral,
) : Primary, SemanticDataHolder<UnaryRealSemanticData>()

data class UnaryModifiablePrimary(
    val unaryOperator: UnaryOperator?,
    val modifiablePrimary: ModifiablePrimary,
) : Primary

enum class BooleanLiteral : Primary { TRUE, FALSE }

data class ModifiablePrimary(
    val variable: Identifier,
    val accessors: List<Accessor>,
) : SemanticDataHolder<ModifiablePrimarySemanticData>()

sealed interface Accessor

data class FieldAccessor(
    val identifier: Identifier,
) : Accessor

data class ArrayAccessor(
    val expression: Expression,
) : Accessor

data class IntegerLiteral(
    val token: Token,
)

data class RealLiteral(
    val token: Token,
)

sealed interface UnaryOperator
sealed interface UnaryRealOperator

enum class UnarySign : UnaryRealOperator, UnaryOperator { PLUS, MINUS }
data object UnaryNot : UnaryOperator
