package com.github.setterwars.compilercourse.codegen.utils

fun Boolean.toInt() = compareTo(false)

fun Int.toBoolean() = this != 0