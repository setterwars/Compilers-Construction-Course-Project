package com.github.setterwars.compilercourse.parser.nodes

data class Body(
    val bodyElements: List<BodyElement>
)

sealed interface BodyElement