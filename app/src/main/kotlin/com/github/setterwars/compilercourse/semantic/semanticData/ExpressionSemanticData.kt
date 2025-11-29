package com.github.setterwars.compilercourse.semantic.semanticData

import com.github.setterwars.compilercourse.semantic.CompileTimeValue

data class ExpressionSemanticData(
    val compileTimeValue: CompileTimeValue? = null,
)