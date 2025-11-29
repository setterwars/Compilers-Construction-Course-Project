package com.github.setterwars.compilercourse.semantic

sealed interface CompileTimeValue

data class CompileTimeInteger(val value: Int) : CompileTimeValue

data class CompileTimeDouble(val value: Double) : CompileTimeValue

data class CompileTimeBoolean(val value: Boolean) : CompileTimeValue