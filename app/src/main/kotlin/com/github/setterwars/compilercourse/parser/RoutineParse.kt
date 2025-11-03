package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser.ParseResult
import com.github.setterwars.compilercourse.parser.nodes.FullRoutineBody
import com.github.setterwars.compilercourse.parser.nodes.ParameterDeclaration
import com.github.setterwars.compilercourse.parser.nodes.Parameters
import com.github.setterwars.compilercourse.parser.nodes.RoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.RoutineHeader
import com.github.setterwars.compilercourse.parser.nodes.SingleExpressionBody


internal fun Parser.parseRoutineDeclaration(index: Int): Result<ParseResult<RoutineDeclaration>> {
    val headerParseResult = parseRoutineHeader(index).getOrElse {
        return Result.failure(it)
    }
    val bodyParseResult = parseRoutineBody(headerParseResult.nextIndex)
    return ParseResult(
        nextIndex = bodyParseResult.getOrNull()?.nextIndex ?: headerParseResult.nextIndex,
        result = RoutineDeclaration(
            header = headerParseResult.result,
            body = bodyParseResult.getOrNull()?.result,
        )
    ).wrapToResult()
}

internal fun Parser.parseRoutineHeader(index: Int): Result<ParseResult<RoutineHeader>> {
    var nextIndex = takeToken(index, TokenType.ROUTINE).getOrElse {
        return Result.failure(it)
    }
    val identifierParseResult = parseIdentifier(nextIndex).getOrElse { return Result.failure(it) }
    nextIndex = takeToken(identifierParseResult.nextIndex, TokenType.LBRACKET).getOrElse {
        return Result.failure(it)
    }
    val parametersParseResult = parseParameters(nextIndex)
    nextIndex = parametersParseResult.getOrNull()?.nextIndex ?: nextIndex
    nextIndex = takeToken(nextIndex, TokenType.RPAREN).getOrElse {
        return Result.failure(it)
    }
    val typeParseResult = takeToken(nextIndex, TokenType.COLON).getOrNull()?.let { i ->
        parseType(i)
    }
    nextIndex = typeParseResult?.getOrNull()?.nextIndex ?: nextIndex
    return ParseResult(
        nextIndex = nextIndex,
        result = RoutineHeader(
            name = identifierParseResult.result,
            parameters = parametersParseResult.getOrNull()?.result ?: Parameters(emptyList()),
            returnType = typeParseResult?.getOrNull()?.result,
        )
    ).wrapToResult()
}

internal fun Parser.parseRoutineBody(index: Int): Result<ParseResult<RoutineBody>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            Parser.ParsePart(
                listOf(
                    ::parseFullRoutineBody,
                    ::parseSingleExpressionBody
                ),
                false
            )
        )
    ) { nodes ->
        match<RoutineBody, RoutineBody>(nodes) { it }
    }
}

internal fun Parser.parseFullRoutineBody(index: Int): Result<ParseResult<FullRoutineBody>> {
    val bodyParseResult = parseBody(index).getOrElse {
        return Result.failure(it)
    }
    return ParseResult(
        nextIndex = bodyParseResult.nextIndex,
        result = FullRoutineBody(bodyParseResult.result),
    ).wrapToResult()
}

internal fun Parser.parseSingleExpressionBody(index: Int): Result<ParseResult<SingleExpressionBody>> {
    val nextIndex = takeToken(index, TokenType.ARROW).getOrElse {
        return Result.failure(it)
    }
    val expressionParseResult = parseExpression(nextIndex).getOrElse {
        return Result.failure(it)
    }
    return ParseResult(
        nextIndex = expressionParseResult.nextIndex,
        result = SingleExpressionBody(
            expression = expressionParseResult.result
        )
    ).wrapToResult()
}

internal fun Parser.parseParameters(index: Int): Result<ParseResult<Parameters>> {
    val firstParameter = parseParameterDeclaration(index).getOrElse {
        return Result.failure(it)
    }
    val restParameters = parseZeroOrMoreTimes(
        index = firstParameter.nextIndex,
        parseFunction = { i ->
            val commaParseResult = takeToken(i, TokenType.COMMA).getOrElse {
                return@parseZeroOrMoreTimes Result.failure(it)
            }
            val parameterDeclarationParseResult = parseParameterDeclaration(commaParseResult).getOrElse {
                return@parseZeroOrMoreTimes Result.failure(it)
            }
            ParseResult(
                nextIndex = parameterDeclarationParseResult.nextIndex,
                result = parameterDeclarationParseResult.result
            ).wrapToResult()
        }
    )
    return ParseResult(
        nextIndex = restParameters.lastOrNull()?.nextIndex ?: firstParameter.nextIndex,
        result = Parameters(
            parameters = listOf(firstParameter.result) + restParameters.map { it.result },
        )
    ).wrapToResult()
}

internal fun Parser.parseParameterDeclaration(index: Int): Result<ParseResult<ParameterDeclaration>> {
    val identifierParseResult = parseIdentifier(index).getOrElse {
        return Result.failure(it)
    }
    val nextIndex = takeToken(identifierParseResult.nextIndex, TokenType.COLON).getOrElse {
        return Result.failure(it)
    }
    val typeParseResult = parseType(nextIndex).getOrElse {
        return Result.failure(it)
    }
    return ParseResult(
        nextIndex = typeParseResult.nextIndex,
        result = ParameterDeclaration(
            name = identifierParseResult.result,
            type = typeParseResult.result,
        )
    ).wrapToResult()
}
