package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import java.io.File
import java.io.FileReader

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: run <source-file> [--semantic]")
        return
    }
    val sourcePath = args[0]

    val file = File(sourcePath)
    val lexer = Lexer(FileReader(file))
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = lexer.nextToken()
        tokens.add(token)
        if (token.tokenType == TokenType.EOF) break
    }
    println(tokens.joinToString("\n"))
//    val parser = Parser(tokens)
//    val program = parser.parse()
//    println("OK! Found ${program.declarations.size} declarations in $sourcePath program")
//
//    if (enableSemantic) {
//        val analyzer = SemanticAnalyzer()
//        val result = analyzer.analyze(program)
//        if (result.errors.isEmpty()) {
//            println("Semantic analysis passed: No errors found :)\nNow modification time!")
//
//            val semanticInfoStore = analyzer.info
//            val modifyingAnalyzer = ModifyingAnalyzer(semanticInfoStore = analyzer.info)
//            val modifiedProgram = modifyingAnalyzer.modifyAnalyze(program)
//
//            val wasmStructureGenerator = WasmStructureGenerator(semanticInfoStore)
//            val wasmModule = wasmStructureGenerator.generate(modifiedProgram)
//
//            val outputDir = File("output")
//            outputDir.mkdirs()
//            val outputFile = File(outputDir, "program.wasm")
//            FileOutputStream(outputFile).use { stream ->
//                stream.write(WasmEncoder.encode(wasmModule))
//            }
//            println("Create, well, this:")
//            println(WatPrinter.printModule(wasmModule))
//        } else {
//            println("Semantic analysis failed with ${result.errors.size} error(s):")
//            result.errors.forEach { error -> println("  $error") }
//        }
//    }
}