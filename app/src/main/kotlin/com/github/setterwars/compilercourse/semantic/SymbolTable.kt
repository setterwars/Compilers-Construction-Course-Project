package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.Type

class SymbolTable(val parent: SymbolTable?) {
    private val variables = mutableMapOf<String, ResolvedType>()
    private val types = mutableMapOf<String, ResolvedType>()
    private val routines = mutableMapOf<String, RoutineSymbol>()

    fun declareVariable(name: String, type: ResolvedType) {
        variables[name] = type
    }

    fun declareType(name: String, type: Type) {
        // Store the unresolved type for now, will be resolved during analysis
        types[name] = ResolvedType.Placeholder(name, type)
    }

    fun declareRoutine(name: String, routine: RoutineSymbol) {
        routines[name] = routine
    }

    fun lookupVariable(name: String): ResolvedType? {
        return variables[name] ?: parent?.lookupVariable(name)
    }

    fun lookupType(name: String): ResolvedType? {
        val type = types[name]
        if (type is ResolvedType.Placeholder) {
            // Need to resolve it
            return null // Will be handled by semantic analyzer
        }
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
    val parameterTypes: List<Type>,
    val returnType: Type?,
    val declaration: RoutineDeclaration
)

sealed class ResolvedType {
    object Integer : ResolvedType()
    object Real : ResolvedType()
    object Boolean : ResolvedType()
    object Unknown : ResolvedType()

    data class Array(
        val elementType: ResolvedType,
        val size: Int?
    ) : ResolvedType()

    data class Record(
        val fields: Map<String, ResolvedType>
    ) : ResolvedType()

    data class Placeholder(
        val name: String,
        val unresolvedType: Type
    ) : ResolvedType()
}
