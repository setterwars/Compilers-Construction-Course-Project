package com.github.setterwars.compilercourse.codegen.generator.cell

/**
 * "Variable" - is anything that has reserved place in some cell (globals, locals, memory)
 * For example, function parameter is a variable that is defined in the locals
 */
data class Variable(
    val cellType: CellType,
    val cellValueType: CellValueType,
)
