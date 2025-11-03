package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser
import java.io.File
import java.io.FileReader

fun main(args: Array<String>) {
    val file = File(args[0])
    val lexer = Lexer(FileReader(file))
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = lexer.nextToken()
        tokens.add(token)
        if (token.tokenType == TokenType.EOF) break
    }
    val parser = Parser(tokens)
    val program = parser.parse()
    println("OK! Found ${program.declarations.size} declarations in $file program")
}