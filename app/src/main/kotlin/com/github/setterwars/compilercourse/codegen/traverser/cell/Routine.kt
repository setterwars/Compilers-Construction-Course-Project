package com.github.setterwars.compilercourse.codegen.traverser.cell

data class Routine(
    val name: String,
    val returnValueType: CellValueType?,
    val parameters: List<Parameter>,
    val index: Int,
) {
    data class Parameter(val name: String, val cellValueType: CellValueType)
}