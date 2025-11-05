package com.github.setterwars.compilercourse.parser.nodes

// Marker for semantic info payloads attached to AST nodes
interface SemanticInfo

// Base class for AST nodes that can carry semantic information
abstract class SemanticNode<T : SemanticInfo> {
    var info: T? = null
    fun requireInfo(): T = checkNotNull(info)
}

// --- Concrete info payloads used by the semantic analyzer ---

data class ExpressionSemanticInfo(
    val type: com.github.setterwars.compilercourse.semantic.ResolvedType
) : SemanticInfo


data class RelationSemanticInfo(
    val type: com.github.setterwars.compilercourse.semantic.ResolvedType,
    val compileTimeValue: Boolean?
) : SemanticInfo


data class SimpleSemanticInfo(
    val type: com.github.setterwars.compilercourse.semantic.ResolvedType
) : SemanticInfo


data class FactorSemanticInfo(
    val type: com.github.setterwars.compilercourse.semantic.ResolvedType
) : SemanticInfo


data class ModifiablePrimarySemanticInfo(
    val type: com.github.setterwars.compilercourse.semantic.ResolvedType
) : SemanticInfo


data class RoutineCallSemanticInfo(
    val type: com.github.setterwars.compilercourse.semantic.ResolvedType
) : SemanticInfo


data class IntegerLiteralSemanticInfo(
    val value: Int
) : SemanticInfo


data class RealLiteralSemanticInfo(
    val value: Double
) : SemanticInfo


data class UnaryIntegerSemanticInfo(
    val value: Int
) : SemanticInfo

