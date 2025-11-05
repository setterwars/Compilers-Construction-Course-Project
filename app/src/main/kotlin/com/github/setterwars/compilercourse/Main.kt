package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser
import com.github.setterwars.compilercourse.semantic.ModifyingAnalyzer
import com.github.setterwars.compilercourse.semantic.SemanticAnalyzer
import java.io.File
import java.io.FileReader

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: run <source-file> [--semantic]")
        return
    }
    val sourcePath = args[0]
    val flags = args.drop(1).toSet()
    val enableSemantic = flags.contains("--semantic")

    val file = File(sourcePath)
    val lexer = Lexer(FileReader(file))
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = lexer.nextToken()
        tokens.add(token)
        if (token.tokenType == TokenType.EOF) break
    }
    val parser = Parser(tokens)
    val program = parser.parse()
    println("OK! Found ${program.declarations.size} declarations in $sourcePath program")

    if (enableSemantic) {
        val analyzer = SemanticAnalyzer()
        val result = analyzer.analyze(program)
        if (result.errors.isEmpty()) {
            println("Semantic analysis passed: No errors found :)\nNow modification time!")

            val modifyingAnalyzer = ModifyingAnalyzer(semanticInfoStore = analyzer.info)
            val modifiedProgram = modifyingAnalyzer.modifyAnalyze(program)
            val a = 20
        } else {
            println("Semantic analysis failed with ${result.errors.size} error(s):")
            result.errors.forEach { error -> println("  $error") }
        }
    }
}