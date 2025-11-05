package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration

/**
 * - Basically a stack of tables
 *
 * Root scope: <variables, types, routines> <---(parent)--- Scope 1: <variables, types, routines> etc.
 *
 * - Scope contains main three entities:
 *
 * variables = Map<String, ResolvedType>
 * types: Map<String, ResolvedType>
 * routines: Map<String, RoutineSymbol> (only for root scope this map is non-empty)
 */
class SymbolTable(val parent: SymbolTable?) {
    private val variables = mutableMapOf<String, ResolvedType>()
    private val types = mutableMapOf<String, ResolvedType>()
    private val routines = mutableMapOf<String, RoutineSymbol>()

    @Deprecated("use addDeclaredVariable")
    fun declareVariable(name: String, type: ResolvedType) {
        variables[name] = type
    }

    fun addDeclaredVariable(name: String, type: ResolvedType) {
        variables[name] = type
    }

    @Deprecated("use addDeclaredType")
    fun declareType(name: String, type: ResolvedType) {
        types[name] = type
    }

    fun addDeclaredType(name: String, type: ResolvedType) {
        types[name] = type
    }

    @Deprecated("Use addDeclaredRoutine")
    fun declareRoutine(name: String, routine: RoutineSymbol) {
        routines[name] = routine
    }

    fun addDeclaredRoutine(name: String, routine: RoutineSymbol) {
        routines[name] = routine
    }

    fun lookupVariable(name: String): ResolvedType? {
        return variables[name] ?: parent?.lookupVariable(name)
    }

    fun lookupType(name: String): ResolvedType? {
        val type = types[name]
        return type ?: parent?.lookupType(name)
    }

    fun lookupRoutine(name: String): RoutineSymbol? {
        return routines[name] ?: parent?.lookupRoutine(name)
    }

    fun isDeclaredInCurrentScope(name: String): Boolean {
        return variables.containsKey(name) || types.containsKey(name) || routines.containsKey(name)
    }
}

data class RoutineSymbol(
    val parameterTypes: List<ResolvedType>,
    val returnType: ResolvedType,
    val declaration: RoutineDeclaration? = null
)

sealed interface ResolvedType {
    sealed interface ResolvedPrimitiveType : ResolvedType
    data object Integer : ResolvedPrimitiveType // follows Kotlin Long
    data object Real : ResolvedPrimitiveType    // follows Kotlin Double
    data object Boolean : ResolvedPrimitiveType

    /** Special: only allowed for routine return types. */
    data object Void : ResolvedType

    @Deprecated("Use UnsizedArray or SizedArray")
    data class Array(
        val elementType: ResolvedType,
        val size: Int?
    ) : ResolvedType

    data class SizedArray(
        val size: Int,
        val elementType: ResolvedType,
    ) : ResolvedType

    data class UnsizedArray(
        val elementType: ResolvedType,
    ) : ResolvedType

    data class Record(
        val fields: Map<String, ResolvedType>
    ) : ResolvedType
}

/** Compile-time constant payloads for primitives. */
sealed interface PrimitiveTypeValue
@JvmInline value class IntValue(val value: Long) : PrimitiveTypeValue
@JvmInline value class BooleanValue(val value: Boolean) : PrimitiveTypeValue
@JvmInline value class RealValue(val value: Double) : PrimitiveTypeValue
