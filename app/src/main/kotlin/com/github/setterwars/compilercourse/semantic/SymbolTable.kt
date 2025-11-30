package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.Type

class SymbolTable(val parent: SymbolTable?) {
    private val variables = mutableMapOf<String, VariableDescription>()
    private val types = mutableMapOf<String, TypeDescription>()
    private val routines = mutableMapOf<String, RoutineSymbol>()

    fun declareVariable(name: String, type: SemanticType, compileTimeValue: CompileTimeValue?) {
        if (variables.containsKey(name)) {
            throw SemanticException("Variable $name was already declared in this scope")
        }
        variables[name] = VariableDescription(type, compileTimeValue)
    }

    fun declareType(name: String, typeNode: Type, semanticType: SemanticType) {
        if (types.containsKey(name)) {
            throw SemanticException("Type $name was already declared in this scope")
        }
        types[name] = TypeDescription(semanticType, typeNode)
    }

    fun declareRoutine(name: String, routine: RoutineSymbol) {
        val checkRoutine = routines[name]
        if (checkRoutine != null) {
            if (checkRoutine.declaration.body != null) {
                throw SemanticException("Routine $name was already declared with body")
            }
            if (routine.declaration.body == null) {
                throw SemanticException("Redeclaration with empty routine body")
            }
            if (routine.parameterTypes != checkRoutine.parameterTypes || routine.returnType != checkRoutine.returnType) {
                throw SemanticException("Redeclaration for routine $name with different header")
            }
        }
        routines[name] = routine
    }

    fun lookupVariable(name: String): VariableDescription? {
        return variables[name] ?: parent?.lookupVariable(name)
    }

    fun lookupType(name: String): TypeDescription? {
        val type = types[name]
        return type ?: parent?.lookupType(name)
    }

    fun lookupRoutine(name: String): RoutineSymbol? {
        return routines[name] ?: parent?.lookupRoutine(name)
    }

    data class VariableDescription(
        val semanticType: SemanticType,
        val compileTimeValue: CompileTimeValue? = null,
    )

    data class TypeDescription(
        val semanticType: SemanticType,
        val underlyingType: Type,
    )
}

data class RoutineSymbol(
    val parameterTypes: List<SemanticType>,
    val returnType: SemanticType?, // null = returns nothing
    val declaration: RoutineDeclaration,
)

sealed interface SemanticType {
    sealed interface SemanticPrimitiveType : SemanticType
    data object Integer : SemanticPrimitiveType
    data object Real : SemanticPrimitiveType
    data object Boolean : SemanticPrimitiveType

    data class Array(
        val size: Int?, // null = variadic parameter
        val elementType: SemanticType,
    ) : SemanticType

    data class Record(
        val fields: List<RecordField>
    ) : SemanticType {
        data class RecordField(
            val name: String,
            val type: SemanticType,
        )
    }
}
