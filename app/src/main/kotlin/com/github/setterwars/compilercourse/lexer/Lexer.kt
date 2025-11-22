package com.github.setterwars.compilercourse.lexer

import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.Reader

class Lexer(
    private val reader: Reader,
) {
    constructor(file: File) : this(
        InputStreamReader(FileInputStream(file))
    )

    val tokenGraph = TokenGraph()

    private var currentCursorPosition: CursorPosition = CursorPosition(0, 0)
    private var previousCursorPosition: CursorPosition = currentCursorPosition
    private var currentSymbol: Char? = null

    private fun getNextSymbol() {
        currentSymbol = reader.read().let { if (it == -1) null else Char(it) }?.also { c ->
            previousCursorPosition = currentCursorPosition
            currentCursorPosition = if (c == '\n') {
                CursorPosition(line = currentCursorPosition.line + 1, column = 0)
            } else {
                CursorPosition(line = currentCursorPosition.line, column = currentCursorPosition.column + 1)
            }
        }
    }

    private var currentSymbolIsProcessedFlag = true
    private var reachedEofBefore = false
    fun nextToken(): Token {
        val begin = if (currentSymbolIsProcessedFlag) currentCursorPosition else previousCursorPosition

        while (true) {
            if (currentSymbolIsProcessedFlag) {
                getNextSymbol()
            }
            val c = currentSymbol
            if (c == null) { // EOF reached
                if (!reachedEofBefore) {
                    reachedEofBefore = true
                    val result = tokenGraph.determine() ?: throw LexerException(
                        message = "Cannot determine token in ${Span(begin, currentCursorPosition)}",
                    )
                    return Token(
                        tokenType = result.first,
                        span = Span(begin, previousCursorPosition),
                        lexeme = result.second,
                    )
                }
                return Token(
                    tokenType = TokenType.EOF,
                    span = Span(begin, currentCursorPosition),
                    lexeme = "<EOF>"
                )
            }

            var returnToken: Token? = null
            if (!tokenGraph.canExpandOnCharacter(c)) {
                val result = tokenGraph.determine()
                val span = Span(begin, previousCursorPosition)
                if (result == null) {
                    throw LexerException(
                        message = "Cannot determine token in $span",
                        span = span
                    )
                }
                returnToken = Token(
                    tokenType = result.first,
                    span = span,
                    lexeme = result.second,
                )
                tokenGraph.feed(c) // reset
                currentSymbolIsProcessedFlag = false
            } else {
                tokenGraph.feed(c)
                currentSymbolIsProcessedFlag = true
            }
            returnToken?.let { return it }
        }
    }

    companion object {
        class LexerException(message: String? = null, span: Span? = null) : RuntimeException(message)
    }
}