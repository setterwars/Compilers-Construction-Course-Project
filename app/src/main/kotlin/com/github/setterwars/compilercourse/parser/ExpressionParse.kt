package com.github.setterwars.compilercourse.parser

    import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser.ParsePart
import com.github.setterwars.compilercourse.parser.Parser.ParseResult
import com.github.setterwars.compilercourse.parser.nodes.Accessor
import com.github.setterwars.compilercourse.parser.nodes.ArrayAccessor
import com.github.setterwars.compilercourse.parser.nodes.BooleanLiteral
import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.ExpressionInParenthesis
import com.github.setterwars.compilercourse.parser.nodes.ExpressionOperator
import com.github.setterwars.compilercourse.parser.nodes.Factor
import com.github.setterwars.compilercourse.parser.nodes.FactorOperator
import com.github.setterwars.compilercourse.parser.nodes.FieldAccessor
import com.github.setterwars.compilercourse.parser.nodes.Identifier
import com.github.setterwars.compilercourse.parser.nodes.IntegerLiteral
import com.github.setterwars.compilercourse.parser.nodes.ModifiablePrimary
import com.github.setterwars.compilercourse.parser.nodes.Primary
import com.github.setterwars.compilercourse.parser.nodes.RealLiteral
import com.github.setterwars.compilercourse.parser.nodes.Relation
import com.github.setterwars.compilercourse.parser.nodes.RelationOperator
import com.github.setterwars.compilercourse.parser.nodes.Simple
import com.github.setterwars.compilercourse.parser.nodes.SimpleOperator
import com.github.setterwars.compilercourse.parser.nodes.Summand
import com.github.setterwars.compilercourse.parser.nodes.UnaryInteger
import com.github.setterwars.compilercourse.parser.nodes.UnaryModifiablePrimary
import com.github.setterwars.compilercourse.parser.nodes.UnaryNot
import com.github.setterwars.compilercourse.parser.nodes.UnaryOperator
import com.github.setterwars.compilercourse.parser.nodes.UnaryReal
import com.github.setterwars.compilercourse.parser.nodes.UnaryRealOperator
import com.github.setterwars.compilercourse.parser.nodes.UnarySign


internal fun Parser.parseUnarySign(index: Int): Result<ParseResult<UnarySign>> {
    return parseAnyTokenOf(index, listOf(TokenType.PLUS, TokenType.MINUS)) { token ->
        return@parseAnyTokenOf when (token.tokenType) {
            TokenType.PLUS -> UnarySign.PLUS
            TokenType.MINUS -> UnarySign.MINUS
            else -> null
        }
    }
}

internal fun Parser.parseUnaryNot(index: Int): Result<ParseResult<UnaryNot>> {
    return parseToken(index, TokenType.NOT) { token ->
        return@parseToken when (token.tokenType) {
            TokenType.NOT -> UnaryNot
            else -> null
        }
    }
}

internal fun Parser.parseUnaryOperator(index: Int): Result<ParseResult<UnaryOperator>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            ParsePart(
                listOf(::parseUnaryNot, ::parseUnarySign),
                false
            )
        )
    ) { nodes ->
        match<UnaryOperator, UnaryOperator>(nodes) { it }
    }
}

internal fun Parser.parseUnaryRealOperator(index: Int): Result<ParseResult<UnaryRealOperator>> {
    return parseUnarySign(index)
}

internal fun Parser.parseIntegerLiteral(index: Int): Result<ParseResult<IntegerLiteral>> {
    return parseToken(index, TokenType.INT_LITERAL) { token ->
        return@parseToken when (token.tokenType) {
            TokenType.INT_LITERAL -> IntegerLiteral(token = token)
            else -> null
        }
    }
}

internal fun Parser.parseRealLiteral(index: Int): Result<ParseResult<RealLiteral>> {
    return parseToken(index, TokenType.REAL_LITERAL) { token ->
        return@parseToken when (token.tokenType) {
            TokenType.REAL_LITERAL -> RealLiteral(token = token)
            else -> null
        }
    }
}

internal fun Parser.parseBooleanLiteral(index: Int): Result<ParseResult<BooleanLiteral>> {
    return parseAnyTokenOf(index, listOf(TokenType.TRUE, TokenType.FALSE)) { token ->
        when (token.tokenType) {
            TokenType.TRUE -> BooleanLiteral.TRUE
            TokenType.FALSE -> BooleanLiteral.FALSE
            else -> null
        }
    }
}

internal fun Parser.parseUnaryInteger(index: Int): Result<ParseResult<UnaryInteger>> {
    return combineParseFunctions(
        index,
        listOf(
            ParsePart(::parseUnaryOperator, true),
            ParsePart(::parseIntegerLiteral, false)
        )
    ) { nodes ->
        match<UnaryInteger, IntegerLiteral>(nodes) { UnaryInteger(null, it) }
            ?: match<UnaryInteger, UnaryOperator, IntegerLiteral>(nodes) { unaryOperator, integerLiteral ->
                UnaryInteger(
                    unaryOperator,
                    integerLiteral
                )
            }
    }
}

internal fun Parser.parseUnaryReal(index: Int): Result<ParseResult<UnaryReal>> {
    return combineParseFunctions(
        index,
        listOf(
            ParsePart(listOf(::parseUnaryRealOperator), true),
            ParsePart(listOf(::parseRealLiteral), false)
        )
    ) { nodes ->
        match<UnaryReal, RealLiteral>(nodes) { UnaryReal(null, it) }
            ?: match<UnaryReal, UnaryRealOperator, RealLiteral>(nodes) { unaryRealOperator, realLiteral ->
                UnaryReal(
                    unaryRealOperator,
                    realLiteral,
                )
            }
    }
}

internal fun Parser.parseIdentifier(index: Int): Result<ParseResult<Identifier>> {
    return parseToken(index, TokenType.IDENTIFIER) { token ->
        return@parseToken when (token.tokenType) {
            TokenType.IDENTIFIER -> Identifier(token = token)
            else -> null
        }
    }
}

internal fun Parser.parsePrimary(index: Int): Result<ParseResult<Primary>> {
    return combineParseFunctions(
        index,
        parseParts = listOf(
            ParsePart(
                listOf(
                    ::parseUnaryInteger,
                    ::parseUnaryReal,
                    ::parseBooleanLiteral,
                    ::parseUnaryModifiablePrimary,
                    ::parseRoutineCall
                ), false
            )
        )
    ) { nodes ->
        match<Primary, Primary>(nodes) { it }
    }
}

internal fun Parser.parseExpressionOperator(index: Int): Result<ParseResult<ExpressionOperator>> {
    return parseAnyTokenOf(
        index,
        listOf(TokenType.AND, TokenType.OR, TokenType.XOR),
    ) { token ->
        when (token.tokenType) {
            TokenType.AND -> ExpressionOperator.AND
            TokenType.OR -> ExpressionOperator.OR
            TokenType.XOR -> ExpressionOperator.XOR
            else -> null
        }
    }
}

internal fun Parser.parseRelationOperator(index: Int): Result<ParseResult<RelationOperator>> {
    return parseAnyTokenOf(
        index,
        listOf(TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE, TokenType.EQ, TokenType.NE),
    ) { token ->
        when (token.tokenType) {
            TokenType.LT -> RelationOperator.LT
            TokenType.LE -> RelationOperator.LE
            TokenType.GT -> RelationOperator.GT
            TokenType.GE -> RelationOperator.GE
            TokenType.EQ -> RelationOperator.EQ
            TokenType.NE -> RelationOperator.NEQ
            else -> null
        }
    }
}

internal fun Parser.parseSimpleOperator(index: Int): Result<ParseResult<SimpleOperator>> {
    return parseAnyTokenOf(
        index,
        listOf(TokenType.PLUS, TokenType.MINUS),
    ) { token ->
        when (token.tokenType) {
            TokenType.PLUS -> SimpleOperator.PLUS
            TokenType.MINUS -> SimpleOperator.MINUS
            else -> null
        }
    }
}

internal fun Parser.parseFactorOperator(index: Int): Result<ParseResult<FactorOperator>> {
    return parseAnyTokenOf(
        index,
        listOf(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT),
    ) { token ->
        when (token.tokenType) {
            TokenType.STAR -> FactorOperator.PRODUCT
            TokenType.SLASH -> FactorOperator.DIVISION
            TokenType.PERCENT -> FactorOperator.MODULO
            else -> null
        }
    }
}

internal fun Parser.parseExpression(index: Int): Result<ParseResult<Expression>> {
    val relationParseResult = parseRelation(index).getOrElse { return Result.failure(it) }
    val tailRelations = parseZeroOrMoreTimes(
        index = relationParseResult.nextIndex,
        parseFunction = { i ->
            combineParseFunctions(
                index = i,
                parseParts = listOf(
                    ParsePart(::parseExpressionOperator),
                    ParsePart(::parseRelation)
                )
            ) { nodes ->
                match<Pair<ExpressionOperator, Relation>, ExpressionOperator, Relation>(nodes) { expressionOperator, relation ->
                    expressionOperator to relation
                }
            }
        }
    )
    return ParseResult(
        nextIndex = tailRelations.lastOrNull()?.nextIndex ?: relationParseResult.nextIndex,
        result = Expression(
            relation = relationParseResult.result,
            rest = tailRelations.map { it.result.first to it.result.second }
        )
    ).wrapToResult()
}

internal fun Parser.parseRelation(index: Int): Result<ParseResult<Relation>> {
    val simpleParseResult = parseSimple(index).getOrElse { return Result.failure(it) }
    val comparisonParseResult = combineParseFunctions(
        index = simpleParseResult.nextIndex,
        parseParts = listOf(
            ParsePart(::parseRelationOperator),
            ParsePart(::parseSimple)
        )
    ) { nodes ->
        match<Pair<RelationOperator, Simple>, RelationOperator, Simple>(nodes) { relationOperator, simple ->
            relationOperator to simple
        }
    }
    return ParseResult(
        nextIndex = comparisonParseResult.getOrNull()?.nextIndex ?: simpleParseResult.nextIndex,
        result = Relation(
            simple = simpleParseResult.result,
            comparison = comparisonParseResult.getOrNull()?.result
        )
    ).wrapToResult()

}

internal fun Parser.parseSimple(index: Int): Result<ParseResult<Simple>> {
    val factorParseResult = parseFactor(index).getOrElse { return Result.failure(it) }
    val tailFactors = parseZeroOrMoreTimes(
        index = factorParseResult.nextIndex,
        parseFunction = { i ->
            combineParseFunctions(
                index = i,
                parseParts = listOf(
                    ParsePart(::parseSimpleOperator),
                    ParsePart(::parseFactor)
                )
            ) { nodes ->
                match<Pair<SimpleOperator, Factor>, SimpleOperator, Factor>(nodes) { simpleOperator, factor ->
                    simpleOperator to factor
                }
            }
        }
    )
    return ParseResult(
        nextIndex = tailFactors.lastOrNull()?.nextIndex ?: factorParseResult.nextIndex,
        result = Simple(
            factor = factorParseResult.result,
            rest = tailFactors.map { it.result.first to it.result.second }
        )
    ).wrapToResult()
}

internal fun Parser.parseFactor(index: Int): Result<ParseResult<Factor>> {
    val summandParseResult = parseSummand(index).getOrElse { return Result.failure(it) }
    val tailSummands = parseZeroOrMoreTimes(
        index = summandParseResult.nextIndex,
        parseFunction = { i ->
            combineParseFunctions(
                index = i,
                parseParts = listOf(
                    ParsePart(::parseFactorOperator),
                    ParsePart(::parseSummand)
                )
            ) { nodes ->
                match<Pair<FactorOperator, Summand>, FactorOperator, Summand>(nodes) { factorOperator, summand ->
                    factorOperator to summand
                }
            }
        }
    )
    return ParseResult(
        nextIndex = tailSummands.lastOrNull()?.nextIndex ?: summandParseResult.nextIndex,
        result = Factor(
            summand = summandParseResult.result,
            rest = tailSummands.map { it.result.first to it.result.second }
        )
    ).wrapToResult()
}

internal fun Parser.parseSummand(index: Int): Result<ParseResult<Summand>> {
    return combineParseFunctions(
        index,
        listOf(
            ParsePart(listOf(::parsePrimary, ::parseExpressionInParenthesis)),
        )
    ) { nodes ->
        match<Summand, Summand>(nodes) { it }
    }
}

internal fun Parser.parseExpressionInParenthesis(index: Int): Result<ParseResult<ExpressionInParenthesis>> {
    var nextIndex = takeToken(index, TokenType.LPAREN).getOrElse { return Result.failure(it) }
    val expressionParseResult = parseExpression(nextIndex).getOrElse { return Result.failure(it) }
    nextIndex = takeToken(expressionParseResult.nextIndex, TokenType.RPAREN).getOrElse { return Result.failure(it) }
    return ParseResult(
        nextIndex = nextIndex,
        result = ExpressionInParenthesis(expressionParseResult.result)
    ).wrapToResult()
}

internal fun Parser.parseUnaryModifiablePrimary(index: Int): Result<ParseResult<UnaryModifiablePrimary>> {
    return combineParseFunctions(
        index,
        listOf(
            ParsePart(::parseUnaryOperator, true),
            ParsePart(::parseModifiablePrimary, false)
        )
    ) { nodes ->
        match<UnaryModifiablePrimary, ModifiablePrimary>(nodes) {
            UnaryModifiablePrimary(null, it)
        } ?:
        match<UnaryModifiablePrimary, UnaryOperator, ModifiablePrimary>(nodes) {
                unaryOperator, modifiablePrimary ->
            UnaryModifiablePrimary(unaryOperator, modifiablePrimary)
        }
    }
}

internal fun Parser.parseModifiablePrimary(index: Int): Result<ParseResult<ModifiablePrimary>> {
    val identifierParseResult = parseIdentifier(index).getOrElse { return Result.failure(it) }
    val accessors = parseZeroOrMoreTimes(
        identifierParseResult.nextIndex,
        parseFunction = { i ->
            combineParseFunctions(
                index = i,
                parseParts = listOf(
                    ParsePart(listOf(::parseFieldAccessor, ::parseArrayAccessor), false)
                )
            ) { nodes ->
                match<Accessor, Accessor>(nodes) { it }
            }
        }
    )
    return ParseResult(
        nextIndex = accessors.lastOrNull()?.nextIndex ?: identifierParseResult.nextIndex,
        result = ModifiablePrimary(
            variable = identifierParseResult.result,
            accessors = accessors.map { it.result }
        )
    ).wrapToResult()
}

internal fun Parser.parseFieldAccessor(index: Int): Result<ParseResult<FieldAccessor>> {
    val nextIndex = takeToken(index, TokenType.DOT).getOrElse { return Result.failure(it) }
    val identifierParseResult = parseIdentifier(nextIndex).getOrElse { return Result.failure(it) }
    return Result.success(
        ParseResult(
            nextIndex = identifierParseResult.nextIndex,
            result = FieldAccessor(identifier = identifierParseResult.result)
        )
    )
}

internal fun Parser.parseArrayAccessor(index: Int): Result<ParseResult<ArrayAccessor>> {
    var nextIndex = takeToken(index, TokenType.LBRACKET).getOrElse { return Result.failure(it) }
    val expressionParseResult = parseExpression(nextIndex).getOrElse { return Result.failure(it) }
    nextIndex = takeToken(expressionParseResult.nextIndex, TokenType.RBRACKET).getOrElse { return Result.failure(it) }
    return Result.success(
        ParseResult(
            nextIndex = nextIndex,
            result = ArrayAccessor(expression = expressionParseResult.result),
        )
    )
}
