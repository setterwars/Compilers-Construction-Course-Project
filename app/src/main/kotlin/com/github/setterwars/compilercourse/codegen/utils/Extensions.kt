package com.github.setterwars.compilercourse.codegen.utils

import com.github.setterwars.compilercourse.parser.nodes.Identifier

fun Boolean.toInt() = compareTo(false)

fun Int.toBoolean() = this != 0

fun Identifier.name() = this.token.lexeme

fun randomString64(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..64)
        .map { chars.random() }
        .joinToString("")
}