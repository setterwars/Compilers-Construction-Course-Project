package com.github.setterwars.compilercourse.semantic.extensionNodes

import com.github.setterwars.compilercourse.parser.nodes.Statement

data class RemovedStatement(
    val removedStatementClassname: String?,
) : Statement
