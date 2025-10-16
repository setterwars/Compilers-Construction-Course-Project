package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.parser.Parser
import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.dump
import com.github.setterwars.compilercourse.parser.utils.createMermaidDiagram
import java.io.File
import java.io.FileWriter

private data class CliOptions(
    val inputPath: String,
    val outputFile: String?,     // --file
    val truncate: Boolean?,      // --truncate
    val maxDepth: Int?           // --max-depth
)

fun main(args: Array<String>) {
    val opts = parseArgs(args) ?: run {
        printUsage()
        return
    }

    val lexer = Lexer(File(opts.inputPath))
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = lexer.nextToken()
        tokens.add(token)
        if (token.tokenType == TokenType.EOF) break
    }

    val parser = Parser(tokens)
    val ast = parser.parseTokens()

    if (opts.outputFile == null) {
        println(ast.dump())
    } else {
        // Write Mermaid diagram to --file path
        val file = File(opts.outputFile)
        file.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
        FileWriter(file).use { fw ->
            val needTruncation = opts.truncate ?: false
            val maxDepth = opts.maxDepth ?: -1
            createMermaidDiagram(fw, ast, needTruncation, maxDepth = maxDepth)
        }
    }
}

/**
 * Parse CLI args.
 * Positional: first non-flag is the input path (required).
 * Flags:
 *   --file=PATH
 *   --truncate=true|false      (optional → null if absent)
 *   --max-depth=INT            (optional → null if absent)
 */
private fun parseArgs(args: Array<String>): CliOptions? {
    var inputPath: String? = null
    var outputFile: String? = null
    var truncate: Boolean? = null
    var maxDepth: Int? = null

    for (raw in args) {
        if (!raw.startsWith("--")) {
            // First positional arg = input path
            if (inputPath == null) {
                inputPath = raw
            } else {
                // Extra positional args are not expected; treat as error
                return null
            }
            continue
        }

        val (key, valueRaw) = splitFlag(raw) ?: return null
        val value = valueRaw?.trimQuotes()

        when (key) {
            "file" -> {
                // Allow empty string (means “create/overwrite empty filename” – typically not useful)
                outputFile = value
            }
            "truncate" -> {
                if (value == null) return null
                truncate = value.toBooleanStrictOrNull() ?: return null
            }
            "max-depth" -> {
                if (value == null) return null
                maxDepth = value.toIntOrNull() ?: return null
            }
            else -> return null
        }
    }

    if (inputPath == null) return null

    return CliOptions(
        inputPath = inputPath!!,
        outputFile = outputFile,
        truncate = truncate,
        maxDepth = maxDepth
    )
}

private fun splitFlag(arg: String): Pair<String, String?>? {
    if (!arg.startsWith("--")) return null
    val withoutDashes = arg.removePrefix("--")
    val idx = withoutDashes.indexOf('=')
    return if (idx >= 0) {
        val key = withoutDashes.substring(0, idx)
        val value = withoutDashes.substring(idx + 1)
        if (key.isBlank()) null else key to value
    } else {
        if (withoutDashes.isBlank()) null else withoutDashes to null
    }
}

private fun String.trimQuotes(): String =
    if ((startsWith('"') && endsWith('"')) || (startsWith('\'') && endsWith('\''))) {
        substring(1, length - 1)
    } else this

private fun printUsage() {
    println(
        """
        Usage:
          program <input-path> [--file=PATH] [--truncate=true|false] [--max-depth=INT]
        
        Notes:
          - <input-path> is required and must be the first positional argument.
          - --file writes a Mermaid diagram to PATH; if omitted, the AST is printed.
          - --truncate and --max-depth are optional. If omitted, they are treated as null.
          - If you need max-depth to affect diagram generation, thread it into createMermaidDiagram.
        """.trimIndent()
    )
}