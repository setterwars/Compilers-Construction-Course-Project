package com.github.setterwars.compilercourse.semantic.extensionNodes

import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.Factor
import com.github.setterwars.compilercourse.parser.nodes.Primary
import com.github.setterwars.compilercourse.parser.nodes.Relation
import com.github.setterwars.compilercourse.parser.nodes.Simple
import com.github.setterwars.compilercourse.semantic.PrimitiveTypeValue

data class CalculatedPrimitiveValue(
    val value: PrimitiveTypeValue,
) : Primary {
    companion object {
        fun asExpression(v: PrimitiveTypeValue) = Expression(
            relation = Relation(
                Simple(Factor(CalculatedPrimitiveValue(v), rest = null), rest = null),
                comparison = null
            ),
            rest = null
        )
    }
}
