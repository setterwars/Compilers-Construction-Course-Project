package com.github.setterwars.compilercourse.parser.nodes

import com.github.setterwars.compilercourse.semantic.PrimitiveTypeValue
import com.github.setterwars.compilercourse.semantic.ResolvedType

// Marker for semantic info payloads attached to AST nodes
interface SemanticInfo

/** Enriched to carry compile-time constant (when available). */
data class ExpressionSemanticInfo(
    val type: ResolvedType,
    val const: PrimitiveTypeValue?
) : SemanticInfo

/** Enriched to carry compile-time constant (boolean or numeric when no comparison). */
data class RelationSemanticInfo(
    val type: ResolvedType,
    val const: PrimitiveTypeValue?
) : SemanticInfo

/** Enriched to carry compile-time constant. */
data class SimpleSemanticInfo(
    val type: ResolvedType,
    val const: PrimitiveTypeValue?
) : SemanticInfo

/** Enriched to carry compile-time constant. */
data class FactorSemanticInfo(
    val type: ResolvedType,
    val const: PrimitiveTypeValue?
) : SemanticInfo

data class ModifiablePrimarySemanticInfo(
    val type: ResolvedType
) : SemanticInfo

data class RoutineCallSemanticInfo(
    val type: ResolvedType
) : SemanticInfo

/** Use Long to match integer domain. */
data class IntegerLiteralSemanticInfo(
    val value: Long
) : SemanticInfo

data class RealLiteralSemanticInfo(
    val value: Double
) : SemanticInfo

data class UnaryIntegerSemanticInfo(
    val value: Long
) : SemanticInfo

data class TypeSemanticInfo(
    val resolvedType: ResolvedType,
) : SemanticInfo