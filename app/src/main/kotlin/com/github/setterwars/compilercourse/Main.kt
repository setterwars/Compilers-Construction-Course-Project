package com.github.setterwars.compilercourse

import java.io.File
import java.util.Vector

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: program <pathname>")
        return
    }

    var VectorOfTokens: Vector<Token> = Vector<Token>()

    val pathname = args[0]
    val file = File(pathname)
    val lexer = Lexer(File(pathname))
    while (true) {
        val token = lexer.nextToken()
        println("$token")
        VectorOfTokens.add(token)
        if (token.tokenType == TokenType.EOF) {
            break
        }
    }
    var ListofTokens: List<Token> = VectorOfTokens.toList()
    var parser = Parser(ListofTokens)
    var ast = parser.tokensToAst()
    prettyPrint(ast)
    return
}