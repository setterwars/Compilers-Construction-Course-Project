package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser.ParseResult
import com.github.setterwars.compilercourse.parser.nodes.Assignment
import com.github.setterwars.compilercourse.parser.nodes.ForLoop
import com.github.setterwars.compilercourse.parser.nodes.IfStatement
import com.github.setterwars.compilercourse.parser.nodes.PrintStatement
import com.github.setterwars.compilercourse.parser.nodes.Range
import com.github.setterwars.compilercourse.parser.nodes.RoutineCall
import com.github.setterwars.compilercourse.parser.nodes.RoutineCallArgument
import com.github.setterwars.compilercourse.parser.nodes.Statement
import com.github.setterwars.compilercourse.parser.nodes.WhileLoop

internal fun Parser.parseStatement(index: Int): Result<ParseResult<Statement>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            Parser.ParsePart(
                listOf(
                    ::parseAssignment,
                    ::parseRoutineCall,
                    ::parseWhileLoop,
                    ::parseForLoop,
                    ::parseRange,
                    ::parseIfStatement,
                    ::parsePrintStatement,
                ),
                false
            )
        )
    ) { nodes ->
        match<Statement, Statement>(nodes) { it }
    }
}

internal fun Parser.parseAssignment(index: Int): Result<ParseResult<Assignment>> {
    val modifiablePrimaryParseResult = parseModifiablePrimary(index).getOrElse {
        return Result.failure(it)
    }
    val nextIndex = takeToken(modifiablePrimaryParseResult.nextIndex, TokenType.ASSIGN).getOrElse {
        return Result.failure(it)
    }
    val expressionParseResult = parseExpression(
        nextIndex
    ).getOrElse {
        return Result.failure(it)
    }
    return ParseResult(
        nextIndex = expressionParseResult.nextIndex,
        result = Assignment(
            modifiablePrimary = modifiablePrimaryParseResult.result,
            expression = expressionParseResult.result
        )
    ).wrapToResult()
}

internal fun Parser.parseRoutineCall(index: Int): Result<ParseResult<RoutineCall>> {
    val identifierParseResult = parseIdentifier(index).getOrElse {
        return Result.failure(it)
    }
    var nextIndex = takeToken(identifierParseResult.nextIndex, TokenType.LPAREN).getOrElse {
        return Result.failure(it)
    }
    val routineArgumentsParseResult = parseExpression(nextIndex).getOrNull()?.let { firstExpressionParseResult ->
        val restExpressions = parseZeroOrMoreTimes(
            index = firstExpressionParseResult.nextIndex,
            parseFunction = { i ->
                val commaParseResult = takeToken(i, TokenType.COMMA).getOrElse {
                    return@parseZeroOrMoreTimes Result.failure(it)
                }
                val expressionParseResult = parseExpression(commaParseResult).getOrElse {
                    return@parseZeroOrMoreTimes Result.failure(it)
                }
                ParseResult(
                    nextIndex = expressionParseResult.nextIndex,
                    result = RoutineCallArgument(
                        expression = expressionParseResult.result
                    )
                ).wrapToResult()
            }
        )
        ParseResult(
            nextIndex = restExpressions.lastOrNull()?.nextIndex ?: firstExpressionParseResult.nextIndex,
            result = buildList {
                add(RoutineCallArgument(firstExpressionParseResult.result))
                addAll(restExpressions.map { it.result })
            }
        )
    }
    nextIndex = routineArgumentsParseResult?.nextIndex ?: nextIndex
    nextIndex = takeToken(nextIndex, TokenType.RPAREN).getOrElse {
        return Result.failure(it)
    }
    return ParseResult(
        nextIndex = nextIndex,
        result = RoutineCall(
            routineName = identifierParseResult.result,
            arguments = routineArgumentsParseResult?.result ?: emptyList()
        )
    ).wrapToResult()
}

internal fun Parser.parseWhileLoop(index: Int): Result<ParseResult<WhileLoop>> {
    var nextIndex = takeToken(index, TokenType.WHILE).getOrElse {
        return Result.failure(it)
    }
    val expressionParseResult = parseExpression(nextIndex).getOrElse {
        return Result.failure(it)
    }
    nextIndex = takeToken(expressionParseResult.nextIndex, TokenType.LOOP).getOrElse {
        return Result.failure(it)
    }
    val bodyParseResult = parseBody(nextIndex).getOrElse {
        return Result.failure(it)
    }
    nextIndex = takeToken(bodyParseResult.nextIndex, TokenType.END).getOrElse {
        return Result.failure(it)
    }
    return ParseResult(
        nextIndex = nextIndex,
        result = WhileLoop(
            condition = expressionParseResult.result,
            body = bodyParseResult.result
        )
    ).wrapToResult()
}

internal fun Parser.parseForLoop(index: Int): Result<ParseResult<ForLoop>> {
    var nextIndex = takeToken(index, TokenType.FOR).getOrElse {
        return Result.failure(it)
    }
    val identifierParseResult = parseIdentifier(nextIndex).getOrElse {
        return Result.failure(it)
    }
    nextIndex = takeToken(identifierParseResult.nextIndex, TokenType.IN).getOrElse {
        return Result.failure(it)
    }
    val rangeParseResult = parseRange(nextIndex).getOrElse {
        return Result.failure(it)
    }
    val reverseParseResult = parseToken(rangeParseResult.nextIndex, TokenType.REVERSE) { token ->
        when (token.tokenType) {
            TokenType.REVERSE -> true
            else -> null
        }
    }
    nextIndex = reverseParseResult.getOrNull()?.nextIndex ?: nextIndex
    nextIndex = takeToken(nextIndex, TokenType.LOOP).getOrElse {
        return Result.failure(it)
    }
    val bodyParseResult = parseBody(nextIndex).getOrElse {
        return Result.failure(it)
    }
    nextIndex = takeToken(bodyParseResult.nextIndex, TokenType.END).getOrElse {
        return Result.failure(it)
    }
    return ParseResult(
        nextIndex = nextIndex,
        result = ForLoop(
            loopVariable = identifierParseResult.result,
            range = rangeParseResult.result,
            reverse = reverseParseResult.getOrNull()?.result == true,
            body = bodyParseResult.result,
        )
    ).wrapToResult()
}

internal fun Parser.parseRange(index: Int): Result<ParseResult<Range>> {
    val expressionParseResult = parseExpression(index).getOrElse {
        return Result.failure(it)
    }
    val secondExpressionParseResult =
        takeToken(expressionParseResult.nextIndex, TokenType.RANGE).getOrNull()?.let { i ->
            parseExpression(i)
        }?.getOrNull()

    return ParseResult(
        nextIndex = secondExpressionParseResult?.nextIndex ?: expressionParseResult.nextIndex,
        result = Range(
            begin = expressionParseResult.result,
            end = secondExpressionParseResult?.result
        )
    ).wrapToResult()
}

internal fun Parser.parseIfStatement(index: Int): Result<ParseResult<IfStatement>> {
    var nextIndex = takeToken(index, TokenType.IF).getOrElse {
        return Result.failure(it)
    }
    val expressionParseResult = parseExpression(nextIndex).getOrElse {
        return Result.failure(it)
    }
    nextIndex = takeToken(nextIndex, TokenType.THEN).getOrElse {
        return Result.failure(it)
    }
    val bodyParseResult = parseBody(nextIndex).getOrElse {
        return Result.failure(it)
    }
    val elseParseResult = takeToken(bodyParseResult.nextIndex, TokenType.ELSE).getOrNull()?.let { i ->
        parseBody(i)
    }
    nextIndex = elseParseResult?.getOrNull()?.nextIndex ?: nextIndex
    nextIndex = takeToken(nextIndex, TokenType.END).getOrElse {
        return Result.failure(it)
    }
    return ParseResult(
        nextIndex = nextIndex,
        result = IfStatement(
            condition = expressionParseResult.result,
            thenBody = bodyParseResult.result,
            elseBody = elseParseResult?.getOrNull()?.result
        )
    ).wrapToResult()
}

internal fun Parser.parsePrintStatement(index: Int): Result<ParseResult<PrintStatement>> {
    var nextIndex = takeToken(index, TokenType.PRINT).getOrElse {
        return Result.failure(it)
    }
    val firstExpressionParseResult = parseExpression(nextIndex).getOrElse() {
        return Result.failure(it)
    }
    val restExpressionsParseResult = parseZeroOrMoreTimes(
        index = firstExpressionParseResult.nextIndex,
        parseFunction = { i ->
            val commaParseResult = takeToken(i, TokenType.COMMA).getOrElse {
                return@parseZeroOrMoreTimes Result.failure(it)
            }
            val expressionParseResult = parseExpression(commaParseResult).getOrElse {
                return@parseZeroOrMoreTimes Result.failure(it)
            }
            ParseResult(
                nextIndex = expressionParseResult.nextIndex,
                result = expressionParseResult.result
            ).wrapToResult()
        }
    )
    return ParseResult(
        nextIndex = nextIndex,
        result = PrintStatement(
            expression = firstExpressionParseResult.result,
            rest = restExpressionsParseResult.map { it.result },
        )
    ).wrapToResult()
}