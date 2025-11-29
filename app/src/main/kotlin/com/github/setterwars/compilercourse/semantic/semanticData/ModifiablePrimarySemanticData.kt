package com.github.setterwars.compilercourse.semantic.semanticData

import com.github.setterwars.compilercourse.semantic.CompileTimeValue

data class ModifiablePrimarySemanticData(
    val compileTimeValue: CompileTimeValue? = null,
)