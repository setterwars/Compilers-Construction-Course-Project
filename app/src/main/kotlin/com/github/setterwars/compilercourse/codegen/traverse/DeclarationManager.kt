package com.github.setterwars.compilercourse.codegen.traverse

class DeclarationManager(private val memoryManager: MemoryManager) {

    data class RoutineDescription(
        val name: String,
        val orderIndex: Int,
        val returnValue: StackValue?, // null = empty stack when finished
        val parameters: List<VariableDescription>,
    )

    data class VariableDescription(
        val name: String,
        val cellType: CellType,
        val address: Int,
    )

    data class TypeDescription(val name: String, val cellType: CellType)

    val allVariables = mutableListOf<VariableDescription>()

    val routines = mutableMapOf<String, RoutineDescription>()
    val variables = mutableListOf<MutableMap<String, VariableDescription>>()
    val types = mutableListOf<MutableMap<String, TypeDescription>>()

    // TODO: fix parameter share for routine calls
    fun declareRoutine(name: String, returnValue: StackValue?, parameters: List<Pair<String, CellType>>) {
        val paramsList = mutableListOf<VariableDescription>()
        for ((name, cellType) in parameters) {
            paramsList.add(
                VariableDescription(
                    name = name,
                    cellType = cellType,
                    address = memoryManager.getCurrentPointer()
                )
            )
            memoryManager.advance(cellType.bytesSize)
        }
        routines[name] = RoutineDescription(
            name = name,
            orderIndex = routines.size,
            returnValue = returnValue,
            parameters = paramsList,
        )
    }

    fun getRoutineOrNull(name: String): RoutineDescription? {
        return routines[name]
    }

    fun getRoutine(name: String): RoutineDescription {
        return routines[name]!!
    }

    fun declareFunctionVariables(name: String) {
        for (variableDescription in routines[name]!!.parameters) {
            variables.last()[variableDescription.name] = variableDescription
            allVariables.add(variableDescription)
        }
    }

    fun declareVariable(name: String, cellType: CellType) {
        val vd = VariableDescription(
            name = name,
            cellType = cellType,
            address = memoryManager.getCurrentPointer()
        )
        variables.last()[name] = vd
        allVariables.add(vd)
        memoryManager.advance(cellType.bytesSize)
    }

    fun getVariable(name: String): VariableDescription {
        for (i in (variables.size - 1) downTo 0) {
            return variables[i][name] ?: continue
        }
        throw CodegenException()
    }

    fun declareType(name: String, cellType: CellType) {
        types.last()[name] = TypeDescription(
            name = name,
            cellType = cellType,
        )
    }

    fun getType(name: String): TypeDescription {
        for (i in types.size - 1 downTo 0) {
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