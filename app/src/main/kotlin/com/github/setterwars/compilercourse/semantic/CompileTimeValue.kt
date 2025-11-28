package com.github.setterwars.compilercourse.semantic

interface CompileTimeValue

data class CompileTimeInteger(val value: Int)

data class CompileTimeDouble(val value: Double)

data class CompileTimeBoolean(val value: Boolean)