package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser.ParseResult
import com.github.setterwars.compilercourse.parser.nodes.ArrayType
import com.github.setterwars.compilercourse.parser.nodes.DeclaredType
import com.github.setterwars.compilercourse.parser.nodes.PrimitiveType
import com.github.setterwars.compilercourse.parser.nodes.RecordType
import com.github.setterwars.compilercourse.parser.nodes.Type
import com.github.setterwars.compilercourse.parser.nodes.UserType

internal fun Parser.parseType(index: Int): Result<ParseResult<Type>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            Parser.ParsePart(
                listOf(
                    ::parsePrimitiveType,
                    ::parseUserType,
                    ::parseDeclaredType
                ),
                false
            )
        )
    ) { nodes ->
        match<Type, Type>(nodes) { it }
    }
}

internal fun Parser.parsePrimitiveType(index: Int): Result<ParseResult<PrimitiveType>> {
    return parseAnyTokenOf(
        index = index,
        tokenTypes = listOf(TokenType.INTEGER, TokenType.REAL, TokenType.BOOLEAN)
    ) { token ->
        when (token.tokenType) {
            TokenType.INTEGER -> PrimitiveType.INTEGER
            TokenType.REAL -> PrimitiveType.REAL
            TokenType.BOOLEAN -> PrimitiveType.BOOLEAN
            else -> null
        }
    }
}

internal fun Parser.parseDeclaredType(index: Int): Result<ParseResult<DeclaredType>> {
    val identifierParseResult = parseIdentifier(index).getOrElse { return Result.failure(it) }
    return ParseResult(
        nextIndex = identifierParseResult.nextIndex,
        result = DeclaredType(identifierParseResult.result)
    ).wrapToResult()
}

internal fun Parser.parseUserType(index: Int): Result<ParseResult<UserType>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            Parser.ParsePart(listOf(::parseArrayType, ::parseRecordType), false)
        )
    ) { nodes ->
        match<UserType, UserType>(nodes) { it }
    }
}

internal fun Parser.parseArrayType(index: Int): Result<ParseResult<ArrayType>> {
    var nextIndex = takeToken(index, TokenType.ARRAY).getOrElse { return Result.failure(it) }
    nextIndex = takeToken(nextIndex, TokenType.LBRACKET).getOrElse { return Result.failure(it) }
    val expressionParseResult = parseExpression(nextIndex)
    nextIndex = expressionParseResult.getOrNull()?.nextIndex ?: nextIndex
    nextIndex = takeToken(nextIndex, TokenType.RBRACKET).getOrElse { return Result.failure(it) }
    val typeParseResult = parseType(nextIndex).getOrElse { return Result.failure(it) }
    return ParseResult(
        nextIndex = typeParseResult.nextIndex,
        result = ArrayType(
            expressionInBrackets = expressionParseResult.getOrNull()?.result,
            type = typeParseResult.result,
        )
    ).wrapToResult()
}

internal fun Parser.parseRecordType(index: Int): Result<ParseResult<RecordType>> {
    var nextIndex = takeToken(index, TokenType.RECORD).getOrElse { return Result.failure(it) }
    val variableDeclarations = parseZeroOrMoreTimes(
        index = nextIndex,
        parseFunction = { i ->
            parseVariableDeclaration(i)
        }
    )
    nextIndex = takeToken(
        index = variableDeclarations.lastOrNull()?.nextIndex ?: nextIndex,
        TokenType.END
    ).getOrElse { return Result.failure(it) }
    return ParseResult(
        nextIndex = nextIndex,
        result = RecordType(
            declarations = variableDeclarations.map { it.result }
        )
    ).wrapToResult()
}