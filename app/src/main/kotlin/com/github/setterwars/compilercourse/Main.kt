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
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = lexer.nextToken()
        tokens.add(token)
        if (token.tokenType == TokenType.EOF) {
            break
        }
    }
    val parser = Parser(tokens)
    val ast = parser.tokensToAst()
    prettyPrint(ast)
    return
}