package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.parser.Parser
import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.dump
import com.github.setterwars.compilercourse.parser.utils.createMermaidDiagram
import java.io.File
import java.io.FileWriter

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: program <pathname> [<file for mermaid diagram>]")
        return
    }

    val pathname = args[0]
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
    val ast = parser.parseTokens()

    if (args.size < 2) {
        ast.dump()
    } else {
        val file = File(args[1])
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        val fileWriter = FileWriter(file)
        val needTruncation = args.size >= 3 && args[2].toBooleanStrictOrNull() == true
        createMermaidDiagram(fileWriter, ast, needTruncation)
        fileWriter.close()
    }
    return
}