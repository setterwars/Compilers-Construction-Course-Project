package com.github.setterwars.compilercourse.codegen.traverse

class DeclarationManager(private val memoryManager: MemoryManager) {

    private var globalVariablesCount = 0

    data class RoutineDescription(
        val name: String,
        val orderIndex: Int,
        val returnValue: StackValue?, // null = empty stack when finished
    )

    data class VariableDescription(
        val name: String,
        val address: Int,
        val data: CodegenData,
    )

    data class TypeDescription(val name: String, val data: CodegenData)

    private val routines = mutableMapOf<String, RoutineDescription>()
    private val variables = mutableListOf<MutableMap<String, VariableDescription>>()
    private val types = mutableListOf<MutableMap<String, TypeDescription>>()

    fun declareRoutine(name: String, returnValue: StackValue?) {
        routines[name] = RoutineDescription(
            name = name,
            orderIndex = routines.size,
            returnValue = returnValue,
        )
    }
    fun getRoutineOrNull(name: String): RoutineDescription? {
        return routines[name]
    }
    fun getRoutine(name: String): RoutineDescription {
        return routines[name]!!
    }

    fun declareVariable(name: String, data: CodegenData) {
        variables.last()[name] = VariableDescription(
            name = name,
            address = memoryManager.getCurrentPointer(),
            data = data,
        )
        memoryManager.advance(data.bytesSize)
    }
    fun getVariable(name: String): VariableDescription {
        for (i in (variables.size - 1)..0) {
            return variables[i][name] ?: continue
        }
        throw CodegenException()
    }

    fun declareType(name: String, data: CodegenData) {
        types.last()[name] = TypeDescription(
            name = name,
            data = data,
        )
    }
    fun getType(name: String): TypeDescription {
        for (i in types.size - 1 .. 0) {
            return types[i][name] ?: continue
        }
        throw CodegenException()
    }

    fun newScope() {
        variables.add(mutableMapOf())
        types.add(mutableMapOf())
    }

    fun exitScope() {
        variables.removeLast()
        types.removeLast()
    }

    fun nestLevel(): Int {
        return variables.size - 1
    }
}