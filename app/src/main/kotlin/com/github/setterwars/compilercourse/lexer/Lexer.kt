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

    private var currentLine = 0
    private var currentColumn = 0
    private var currentSymbol: Char? = null
    private var isCurrentSymbolProcessed = true
    private var wasEndBefore = false

    private fun markCurrentSymbolAsProcessed() {
        isCurrentSymbolProcessed = true
        if (currentSymbol == '\n') {
            currentLine++
            currentColumn = 0
        } else {
            currentColumn++
        }
    }

    private fun getCurrentSymbol(): Char? { // null if EOF reached
        if (isCurrentSymbolProcessed) {
            isCurrentSymbolProcessed = false
            currentSymbol = reader.read().let { if (it == -1) null else Char(it) }
        }
        return currentSymbol
    }

    fun nextToken(): Token {
        while (true) {
            // Получаем текущий символ
            val c = getCurrentSymbol()
            if (c == null) {
                if (wasEndBefore) {
                    // Граф уже кушал EOF, значит теперь всегда просто возвращаем EOF
                    return Token(
                        tokenType = TokenType.EOF,
                        span = Span(line = currentLine, firstColumn = currentColumn, lastColumn = currentColumn),
                        lexeme = "<EOF>"
                    )
                }
                // Иначе мы скормим графу EOF, и пометим во флаге, что мы это сделали
                wasEndBefore = true
            }
            val result = tokenGraph.feed(c)

            // Если result null (то есть на данный момент нельзя точно определить токен),
            // или граф подавился на каком-то пробеле (табе), то забиваем хрен и топаем дальше
            if (result == null || (c != '\n' && c?.isWhitespace() == true && result.first == null || result.second == null)) {
                markCurrentSymbolAsProcessed()
            } else {
                val (tokenType, lexeme) = result
                val span = Span(
                    line = currentLine,
                    firstColumn = (currentColumn - (lexeme?.length
                        ?: 0)).coerceAtLeast(0), // TODO: добавить нормальное расположение new line-ов
                    lastColumn = (currentColumn - 1).coerceAtLeast(0),
                )
                // Если tokenType null, то граф подавился на этом символе (то есть граф не смог отыскать ни одного
                // подходящего токена) и текущий символ (c) - не пробельный. Значит программа херня и мы кидаем ошибку
                if (tokenType == null || lexeme == null) {
                    throw LexerException(
                        message = "Cannot parse lexeme $lexeme from column ${span.firstColumn} to ${span.lastColumn} on line ${span.line}",
                        span = span
                    )
                }

                // Ну и наконец, мы нашли верный токен - возвращаем его
                if (tokenType != null && lexeme != null) {
                    return Token(
                        tokenType = tokenType,
                        span = span,
                        lexeme = lexeme
                    )
                }
            }
        }
    }

    companion object {
        class LexerException(message: String? = null, span: Span? = null) : RuntimeException(message)
    }
}