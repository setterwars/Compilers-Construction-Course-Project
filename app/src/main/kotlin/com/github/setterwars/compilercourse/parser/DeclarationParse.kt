package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser.ParseResult
import com.github.setterwars.compilercourse.parser.nodes.Declaration
import com.github.setterwars.compilercourse.parser.nodes.Program
import com.github.setterwars.compilercourse.parser.nodes.SimpleDeclaration
import com.github.setterwars.compilercourse.parser.nodes.TypeDeclaration
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclaration
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationNoType
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationWithType

internal fun Parser.parseProgram(index: Int): Result<ParseResult<Program>> {
    val declarations = parseZeroOrMoreTimes(
        index = index,
        parseFunction = { i ->
            parseDeclaration(i)
        }
    )
    return ParseResult(
        nextIndex = declarations.lastOrNull()?.nextIndex ?: index,
        result = Program(declarations.map { it.result })
    ).wrapToResult()
}

internal fun Parser.parseDeclaration(index: Int): Result<ParseResult<Declaration>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            Parser.ParsePart(listOf(::parseSimpleDeclaration, ::parseRoutineDeclaration), false)
        )
    ) { nodes ->
        match<Declaration, Declaration>(nodes) { it }
    }
}

internal fun Parser.parseSimpleDeclaration(index: Int): Result<ParseResult<SimpleDeclaration>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            Parser.ParsePart(listOf(::parseVariableDeclaration, ::parseTypeDeclaration), false)
        )
    ) { nodes ->
        match<SimpleDeclaration, SimpleDeclaration>(nodes) { it }
    }
}

internal fun Parser.parseVariableDeclaration(index: Int): Result<ParseResult<VariableDeclaration>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            Parser.ParsePart(
                listOf(::parseVariableDeclarationWithType, ::parseVariableDeclarationNoType),
                false
            )
        )
    ) { nodes ->
        match<VariableDeclaration, VariableDeclaration>(nodes) { it }
    }
}

internal fun Parser.parseVariableDeclarationWithType(index: Int): Result<ParseResult<VariableDeclarationWithType>> {
    var nextIndex = takeToken(index, TokenType.VAR).getOrElse { return Result.failure(it) }
    val identifierParseResult = parseIdentifier(nextIndex).getOrElse { return Result.failure(it) }
    nextIndex = takeToken(identifierParseResult.nextIndex, TokenType.COLON).getOrElse { return Result.failure(it) }
    val typeParseResult = parseType(nextIndex).getOrElse { return Result.failure(it) }
    val expressionParseResult = takeToken(typeParseResult.nextIndex, TokenType.IS).getOrNull()?.let { index ->
        parseExpression(index)
    }
    return ParseResult(
        nextIndex = expressionParseResult?.getOrNull()?.nextIndex ?: typeParseResult.nextIndex,
        result = VariableDeclarationWithType(
            identifier = identifierParseResult.result,
            type = typeParseResult.result,
            initialValue = expressionParseResult?.getOrNull()?.result
        )
    ).wrapToResult()
}

internal fun Parser.parseVariableDeclarationNoType(index: Int): Result<ParseResult<VariableDeclarationNoType>> {
    var nextIndex = takeToken(index, TokenType.VAR).getOrElse { return Result.failure(it) }
    val identifierParseResult = parseIdentifier(nextIndex).getOrElse { return Result.failure(it) }
    nextIndex = takeToken(identifierParseResult.nextIndex, TokenType.IS).getOrElse { return Result.failure(it) }
    val expressionParseResult = parseExpression(nextIndex).getOrElse { return Result.failure(it) }
    return ParseResult(
        nextIndex = expressionParseResult.nextIndex,
        result = VariableDeclarationNoType(
            identifier = identifierParseResult.result,
            initialValue = expressionParseResult.result
        )
    ).wrapToResult()
}

internal fun Parser.parseTypeDeclaration(index: Int): Result<ParseResult<TypeDeclaration>> {
    var nextIndex = takeToken(index, TokenType.TYPE).getOrElse { return Result.failure(it) }
    val identifierParseResult = parseIdentifier(nextIndex).getOrElse { return Result.failure(it) }
    nextIndex = takeToken(identifierParseResult.nextIndex, TokenType.IS).getOrElse { return Result.failure(it) }
    val typeParseResult = parseType(nextIndex).getOrElse { return Result.failure(it) }
    return ParseResult(
        nextIndex = typeParseResult.nextIndex,
        result = TypeDeclaration(
            identifier = identifierParseResult.result,
            type = typeParseResult.result
        )
    ).wrapToResult()
}