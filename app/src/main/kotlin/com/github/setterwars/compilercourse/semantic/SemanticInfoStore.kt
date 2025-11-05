package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.parser.nodes.*
import java.util.IdentityHashMap

class SemanticInfoStore {
    private val infoByNode: MutableMap<Any, SemanticInfo> = IdentityHashMap()

    fun setExpressionType(node: Expression, type: ResolvedType) {
        infoByNode[node] = ExpressionSemanticInfo(type)
    }

    fun setRelationInfo(node: Relation, type: ResolvedType, compileTimeValue: Boolean?) {
        infoByNode[node] = RelationSemanticInfo(type, compileTimeValue)
    }

    fun setSimpleType(node: Simple, type: ResolvedType) {
        infoByNode[node] = SimpleSemanticInfo(type)
    }

    fun setFactorType(node: Factor, type: ResolvedType) {
        infoByNode[node] = FactorSemanticInfo(type)
    }

    fun setModifiablePrimaryType(node: ModifiablePrimary, type: ResolvedType) {
        infoByNode[node] = ModifiablePrimarySemanticInfo(type)
    }

    fun setRoutineCallType(node: RoutineCall, type: ResolvedType) {
        infoByNode[node] = RoutineCallSemanticInfo(type)
    }

    fun setIntegerLiteralValue(node: IntegerLiteral, value: Int) {
        infoByNode[node] = IntegerLiteralSemanticInfo(value)
    }

    fun setRealLiteralValue(node: RealLiteral, value: Double) {
        infoByNode[node] = RealLiteralSemanticInfo(value)
    }

    fun setUnaryIntegerValue(node: UnaryInteger, value: Int) {
        infoByNode[node] = UnaryIntegerSemanticInfo(value)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : SemanticInfo> get(node: Any): T? = infoByNode[node] as? T
}


