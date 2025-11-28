package com.github.setterwars.compilercourse.codegen.generator.common

import com.github.setterwars.compilercourse.codegen.utils.CodegenException

class Traverser(
    private val memoryManager: MemoryManager
) {
    val scopes = mutableListOf<ScopedEntitiesManager>()
    var currentRoutine: String? = null

    fun newScope() {
        scopes.add(ScopedEntitiesManager(memoryManager))
    }

    fun enterRoutine(name: String) {
        if (currentRoutine == null) {
            throw CodegenException()
        }
        currentRoutine = name
    }

    fun exitRoutine() {
        currentRoutine = null
    }

    fun exitScope() {
        scopes.removeLast()
    }
}