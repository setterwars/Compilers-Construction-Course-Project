package com.github.setterwars.compilercourse.lexer

data class Span(
    val line: Int,
    val firstColumn: Int,
    val lastColumn: Int,
)

data class Token(
    val tokenType: TokenType,
    val span: Span,
    val lexeme: String,
)

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
    SEMICOLON, // ;
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

    // layout
    NEW_LINE, EOF,
}
