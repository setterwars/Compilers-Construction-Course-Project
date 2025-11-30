package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.codegen.bytecode.encoder.WasmEncoder
import com.github.setterwars.compilercourse.codegen.bytecode.encoder.WasmWriter
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WatPrinter
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmIRGenerator
import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser
import com.github.setterwars.compilercourse.semantic.SemanticAnalyzer
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.OutputStreamWriter

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: run <source-file> <compiled-filename>")
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
    val parser = Parser(tokens)
    val program = parser.parse()
    println("OK! Found ${program.declarations.size} declarations in $sourcePath program")

    val analyzer = SemanticAnalyzer()
    analyzer.analyze(program)

    val wasmIRGenerator = WasmIRGenerator()
    val wasmModule = wasmIRGenerator.generateWasmModule(program)
    println(WatPrinter.printModule(wasmModule))

    val encodedModule = WasmEncoder.encode(wasmModule)
    val output = File("output/program.wasm")
    FileOutputStream(output).use { streamWriter ->
        streamWriter.write(encodedModule)
    }
    return
}