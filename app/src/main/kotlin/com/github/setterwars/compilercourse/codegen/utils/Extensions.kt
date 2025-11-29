package com.github.setterwars.compilercourse.codegen.utils

import com.github.setterwars.compilercourse.parser.nodes.Identifier

fun Boolean.toInt() = compareTo(false)

fun Int.toBoolean() = this != 0

fun Identifier.name() = this.token.lexeme