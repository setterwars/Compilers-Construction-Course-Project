package com.github.setterwars.compilercourse.lexer

data class CursorPosition(val line: Int, val column: Int)

data class Span(
    val begin: CursorPosition,
    val end: CursorPosition,
) {
    val line = begin.line
}

data class Token(
    val tokenType: TokenType,
    val span: Span,
    val lexeme: String,
) {
    private fun String.normalize() = this
        .replace("\n", "\\n")
        .replace("\t", "\\t")

    override fun toString(): String {
        return "Token(tokenType=$tokenType, span=$span, lexeme=\"${lexeme.normalize()}\")"
    }
}

enum class TokenType {
    // literals
    IDENTIFIER, // (letter | underscore) [ letter | underscore | digit] *
    INT_LITERAL, // digit+
    REAL_LITERAL, // digit dot digit+
    FALSE, // false
    TRUE, // true

    // punctuation
    COLON, // :
    LBRACKET, // [
    RBRACKET, // ]
    ASSIGN, // :=
    COMMA, // ,
    LPAREN, // (
    RPAREN, // )
    RANGE, // ..
    ARROW, // =>
    AND, // and
    OR, // or
    XOR, // xor
    LT, // <
    LE, // <=
    GT, // >
    GE, // >=
    EQ, // =
    NE, // /=
    PLUS, // +
    MINUS, // -
    STAR, // *
    SLASH, // /
    PERCENT, // %
    DOT, // .
    WHITESPACE, // all consecutive space symbols (except new lines) are collapsed into a single WHITESPACE token

    // keywords
    VAR,
    IS,
    TYPE,
    INTEGER,
    REAL,
    BOOLEAN,
    RECORD,
    END,
    ARRAY,
    WHILE,
    LOOP,
    FOR,
    IN,
    REVERSE,
    IF,
    THEN,
    ELSE,
    PRINT,
    ROUTINE,
    NOT,
    RETURN,

    // layout
    NEW_LINE, EOF,
}
