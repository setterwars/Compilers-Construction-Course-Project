package com.github.setterwars.compilercourse

import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: program <pathname>")
        return
    }

    val pathname = args[0]
    val file = File(pathname)
    val lexer = Lexer(File(pathname))
    while (true) {
        val token = lexer.nextToken()
        println("$token")
        if (token.tokenType == TokenType.EOF) {
            break
        }
    }
    return
}