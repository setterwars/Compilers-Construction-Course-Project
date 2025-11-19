package com.github.setterwars.compilercourse.codegen.traverse

// TODO: add ability to increase pages and generate instructions
class MemoryManager {
    private var pointer: Int = 1
    fun advance(amount: Int) {
        pointer += amount
    }
    fun getCurrentPointer() = pointer
}