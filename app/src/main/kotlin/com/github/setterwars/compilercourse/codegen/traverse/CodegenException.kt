package com.github.setterwars.compilercourse.codegen.traverse

class CodegenException(
    val function: String? = null,
    override val message: String = ""
) : Exception(message)