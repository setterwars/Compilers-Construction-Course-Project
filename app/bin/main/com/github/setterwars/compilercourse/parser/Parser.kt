package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.nodes.Accessor
import com.github.setterwars.compilercourse.parser.nodes.ArrayAccessor
import com.github.setterwars.compilercourse.parser.nodes.ArrayType
import com.github.setterwars.compilercourse.parser.nodes.Assignment
import com.github.setterwars.compilercourse.parser.nodes.BooleanLiteral
import com.github.setterwars.compilercourse.parser.nodes.Declaration
import com.github.setterwars.compilercourse.parser.nodes.DeclaredType
import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.ExpressionInParenthesis
import com.github.setterwars.compilercourse.parser.nodes.ExpressionOperator
import com.github.setterwars.compilercourse.parser.nodes.Factor
import com.github.setterwars.compilercourse.parser.nodes.FieldAccessor
import com.github.setterwars.compilercourse.parser.nodes.ForLoop
import com.github.setterwars.compilercourse.parser.nodes.FullRoutineBody
import com.github.setterwars.compilercourse.parser.nodes.Identifier
import com.github.setterwars.compilercourse.parser.nodes.IfStatement
import com.github.setterwars.compilercourse.parser.nodes.IntegerLiteral
import com.github.setterwars.compilercourse.parser.nodes.ModifiablePrimary
import com.github.setterwars.compilercourse.parser.nodes.ParameterDeclaration
import com.github.setterwars.compilercourse.parser.nodes.Parameters
import com.github.setterwars.compilercourse.parser.nodes.Primary
import com.github.setterwars.compilercourse.parser.nodes.PrimitiveType
import com.github.setterwars.compilercourse.parser.nodes.PrintStatement
import com.github.setterwars.compilercourse.parser.nodes.Program
import com.github.setterwars.compilercourse.parser.nodes.Range
import com.github.setterwars.compilercourse.parser.nodes.RealLiteral
import com.github.setterwars.compilercourse.parser.nodes.RecordType
import com.github.setterwars.compilercourse.parser.nodes.Relation
import com.github.setterwars.compilercourse.parser.nodes.RelationOperator
import com.github.setterwars.compilercourse.parser.nodes.RoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineCall
import com.github.setterwars.compilercourse.parser.nodes.RoutineCallArgument
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.RoutineHeader
import com.github.setterwars.compilercourse.parser.nodes.Simple
import com.github.setterwars.compilercourse.parser.nodes.SimpleDeclaration
import com.github.setterwars.compilercourse.parser.nodes.SimpleOperator
import com.github.setterwars.compilercourse.parser.nodes.SingleExpressionBody
import com.github.setterwars.compilercourse.parser.nodes.Statement
import com.github.setterwars.compilercourse.parser.nodes.Summand
import com.github.setterwars.compilercourse.parser.nodes.Type
import com.github.setterwars.compilercourse.parser.nodes.UnaryInteger
import com.github.setterwars.compilercourse.parser.nodes.UnaryNot
import com.github.setterwars.compilercourse.parser.nodes.UnaryOperator
import com.github.setterwars.compilercourse.parser.nodes.UnaryReal
import com.github.setterwars.compilercourse.parser.nodes.UnaryRealOperator
import com.github.setterwars.compilercourse.parser.nodes.UnarySign
import com.github.setterwars.compilercourse.parser.nodes.UserType
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclaration
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationNoType
import com.github.setterwars.compilercourse.parser.nodes.VariableDeclarationWithType
import com.github.setterwars.compilercourse.parser.nodes.WhileLoop
import java.lang.reflect.Field
import java.util.Stack

class Parser(private val tokens: List<Token>) {
    fun parse(): Program? {
        return null
    }

    private fun getToken(index: Int): Token? {
        return tokens.getOrNull(index)
    }

    private class WrongTokenTypeException(
        private val token: Token?,
        private val expectedTypes: List<TokenType>,
    ) : Exception(
        "Wrong tokenType: in token $token the type is not any of the expected types: ${
            expectedTypes.joinToString(
                ", "
            )
        }"
    ) {
        constructor(
            token: Token?,
            expectedType: TokenType,
        ) : this(token, listOf(expectedType))
    }

    private fun <T> parseAnyTokenOf(
        index: Int,
        tokenTypes: List<TokenType>,
        transform: (Token) -> T?,
    ): Result<ParseResult<T>> {
        val currentIndex = skipNewLines(index)

        tokenTypes.forEach { tokenType ->
            getToken(currentIndex)?.takeIf { it.tokenType == tokenType }?.let {
                transform(it)?.let { node ->
                    return Result.success(ParseResult(currentIndex + 1, node))
                }
            }
        }
        return Result.failure(WrongTokenTypeException(token = getToken(index), tokenTypes))
    }

    private data class ParsePart(
        val alternatingParseFunctions: List<(Int) -> Result<ParseResult<Any>>>,
        val optional: Boolean = false,
    ) {
        constructor(
            parseFunction: (Int) -> Result<ParseResult<Any>>,
            optional: Boolean = false
        ) : this(listOf(parseFunction), optional)
    }

    private fun <T> combineParseFunctions(
        index: Int,
        parseParts: List<ParsePart>,
        transform: (List<Any>) -> T?,
    ): Result<ParseResult<T>> {
        val parsePartsStates = MutableList(size = parseParts.size) { 0 }
        val parsePartsResults = mutableListOf<Pair<Int, ParseResult<Any>>>() // pair (parse part index, result)
        var lastFailure = Result.failure<ParseResult<T>>(Exception())

        //  This is basically recursive bruteforce written with stack and while
        var parsePartsIndex = 0
        while (true) {
            if (parsePartsIndex == parseParts.size) {
                transform(parsePartsResults.map { it.second.result })?.let { finalResult ->
                    return Result.success(
                        ParseResult(
                            nextIndex = parsePartsResults.last().second.nextIndex,
                            result = finalResult,
                        )
                    )
                }
            }

            while (
                parsePartsIndex >= 0 &&
                parsePartsStates[parsePartsIndex] == parseParts[parsePartsIndex].alternatingParseFunctions.size
            ) {
                parsePartsStates[parsePartsIndex] = 0
                if (parsePartsResults.lastOrNull()?.first == parsePartsIndex) {
                    parsePartsResults.removeLast()
                }
                parsePartsIndex--
            }
            if (parsePartsIndex < 0) {
                break
            }

            parsePartsStates[parsePartsIndex]++
            val startIndex = if (parsePartsResults.isEmpty()) {
                index
            } else {
                parsePartsResults.last().second.nextIndex
            }

            val result =
                parseParts[parsePartsIndex].alternatingParseFunctions[parsePartsStates[parsePartsIndex]](startIndex)
            result.onSuccess { parseResult ->
                parsePartsResults.add(parsePartsIndex to parseResult)
                parsePartsIndex++
            }.onFailure {
                lastFailure = Result.failure(it)
            }
        }
        return lastFailure
    }

    private fun takeToken(index: Int, tokenType: TokenType): Result<Int> {
        val startIndex = skipNewLines(index)
        val token = getToken(index)
        if (token?.tokenType == tokenType) {
            return Result.success(startIndex + 1)
        }
        return Result.failure(WrongTokenTypeException(token, tokenType))
    }

    private fun <T> parseZeroOrMoreTimes(
        index: Int,
        parseFunction: (Int) -> Result<ParseResult<T>>
    ): List<ParseResult<T>> {
        val parseResult = mutableListOf<ParseResult<T>>()
        var currentIndex = index
        while (true) {
            parseFunction(currentIndex).onFailure {
                break
            }.onSuccess { (nextIndex, result) ->
                parseResult.add(ParseResult(nextIndex, result))
                currentIndex = nextIndex
            }
        }
        return parseResult
    }

    private inline fun <reified T, reified R1> match(nodes: List<Any>, transform: (R1) -> T): T? {
        if (nodes.size != 1) return null
        val node0 = nodes[0]
        return if (node0 is R1) transform(node0) else null
    }

    private inline fun <reified T, reified R1, reified R2> match(nodes: List<Any>, transform: (R1, R2) -> T): T? {
        if (nodes.size != 2) return null
        val node0 = nodes[0]
        val node1 = nodes[1]
        return if (node0 is R1 && node1 is R2) transform(node0, node1) else null
    }

    private fun <T> parseToken(
        index: Int,
        tokenType: TokenType,
        transform: (Token) -> T?,
    ): Result<ParseResult<T>> {
        return parseAnyTokenOf(index, listOf(tokenType), transform)
    }

    private fun skipNewLines(index: Int): Int {
        var currentIndex = index
        while (currentIndex < tokens.size && getToken(currentIndex)?.tokenType == TokenType.NEW_LINE) {
            currentIndex++
        }
        return currentIndex
    }

    private fun parseUnarySign(index: Int): Result<ParseResult<UnarySign>> {
        return parseAnyTokenOf(index, listOf(TokenType.PLUS, TokenType.MINUS)) { token ->
            return@parseAnyTokenOf when (token.tokenType) {
                TokenType.PLUS -> UnarySign.PLUS
                TokenType.MINUS -> UnarySign.MINUS
                else -> null
            }
        }
    }

    private fun parseUnaryNot(index: Int): Result<ParseResult<UnaryNot>> {
        return parseToken(index, TokenType.NOT) { token ->
            return@parseToken when (token.tokenType) {
                TokenType.NOT -> UnaryNot
                else -> null
            }
        }
    }

    private fun parseUnaryOperator(index: Int): Result<ParseResult<UnaryOperator>> {
        return parseUnarySign(index).onFailure { parseUnaryNot(index) }
    }

    private fun parseUnaryRealOperator(index: Int): Result<ParseResult<UnaryRealOperator>> {
        return parseUnarySign(index)
    }

    private fun parseIntegerLiteral(index: Int): Result<ParseResult<IntegerLiteral>> {
        return parseToken(index, TokenType.INT_LITERAL) { token ->
            return@parseToken when (token.tokenType) {
                TokenType.INT_LITERAL -> IntegerLiteral(token = token)
                else -> null
            }
        }
    }

    private fun parseRealLiteral(index: Int): Result<ParseResult<RealLiteral>> {
        return parseToken(index, TokenType.REAL_LITERAL) { token ->
            return@parseToken when (token.tokenType) {
                TokenType.REAL_LITERAL -> RealLiteral(token = token)
                else -> null
            }
        }
    }

    private fun parseBooleanLiteral(index: Int): Result<ParseResult<BooleanLiteral>> {
        return parseAnyTokenOf(index, listOf(TokenType.TRUE, TokenType.FALSE)) { token ->
            when (token.tokenType) {
                TokenType.TRUE -> BooleanLiteral.TRUE
                TokenType.FALSE -> BooleanLiteral.FALSE
                else -> null
            }
        }
    }

    private fun parseUnaryInteger(index: Int): Result<ParseResult<UnaryInteger>> {
        return combineParseFunctions(
            index,
            listOf(
                ParsePart(listOf(::parseUnaryOperator), true),
                ParsePart(listOf(::parseIntegerLiteral), false)
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

    private fun parseUnaryReal(index: Int): Result<ParseResult<UnaryReal>> {
        return combineParseFunctions(
            index,
            listOf(
                ParsePart(listOf(::parseUnaryRealOperator), true),
                ParsePart(listOf(::parseIntegerLiteral), false)
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

    private fun parseIdentifier(index: Int): Result<ParseResult<Identifier>> {
        return parseToken(index, TokenType.IDENTIFIER) { token ->
            return@parseToken when (token.tokenType) {
                TokenType.IDENTIFIER -> Identifier(token = token)
                else -> null
            }
        }
    }

    private fun parsePrimary(index: Int): Result<ParseResult<Primary>> {
        return combineParseFunctions(
            index,
            parseParts = listOf(
                ParsePart(
                    listOf(
                        ::parseUnaryInteger,
                        ::parseUnaryReal,
                        ::parseBooleanLiteral,
                        ::parseModifiablePrimary,
                        ::parseRoutineCall
                    ), false
                )
            )
        ) { nodes ->
            match<Primary, Primary>(nodes) { it }
        }
    }

    private fun parseExpressionOperator(index: Int): Result<ParseResult<ExpressionOperator>> {
        TODO()
    }

    private fun parseRelationOperator(index: Int): Result<ParseResult<RelationOperator>> {
        TODO()
    }

    private fun parseSimpleOperator(index: Int): Result<ParseResult<SimpleOperator>> {
        TODO()
    }

    private fun parseExpression(index: Int): Result<ParseResult<Expression>> {
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

    private fun parseRelation(index: Int): Result<ParseResult<Relation>> {
        val simpleParseResult = parseSimple(index).getOrElse { return Result.failure(it) }
        val tailSimples = parseZeroOrMoreTimes(
            index = simpleParseResult.nextIndex,
            parseFunction = { i ->
                combineParseFunctions(
                    index = i,
                    parseParts = listOf(
                        ParsePart(::parseRelationOperator),
                        ParsePart(::parseSimple)
                    )
                ) { nodes ->
                    match<Pair<RelationOperator, Simple>, RelationOperator, Simple>(nodes) { relationOperator, simple ->
                        relationOperator to simple
                    }
                }
            }
        )
        return ParseResult(
            nextIndex = tailSimples.lastOrNull()?.nextIndex ?: simpleParseResult.nextIndex,
            result = Relation(
                simple = simpleParseResult.result,
                rest = tailSimples.map { it.result.first to it.result.second }
            )
        ).wrapToResult()
    }

    private fun parseSimple(index: Int): Result<ParseResult<Simple>> {
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

    private fun parseFactor(index: Int): Result<ParseResult<Factor>> {
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

    private fun parseSummand(index: Int): Result<ParseResult<Summand>> {
        TODO()
    }

    private fun parseExpressionInParenthesis(index: Int): Result<ParseResult<ExpressionInParenthesis>> {
        TODO()
    }

    private fun parseModifiablePrimary(index: Int): Result<ParseResult<ModifiablePrimary>> {
        val identifierParseResult = parseIdentifier(index).getOrElse { return Result.failure(it) }
        var currentIndex = identifierParseResult.nextIndex
        val accessors = mutableListOf<Accessor>()
        while (true) {
            val tailParseResult = combineParseFunctions(
                currentIndex,
                listOf(
                    ParsePart(listOf(::parseFieldAccessor, ::parseArrayAccessor), true)
                )
            ) { nodes ->
                match<Accessor, ArrayAccessor>(nodes) { it } ?: match<Accessor, FieldAccessor>(nodes) { it }
            }
            tailParseResult.onFailure {
                break
            }.onSuccess { (nextIndex, accessor) ->
                currentIndex = nextIndex
                accessors.add(accessor)
            }
        }
        return Result.success(
            ParseResult(
                nextIndex = currentIndex,
                result = ModifiablePrimary(variable = identifierParseResult.result, accessors = accessors)
            )
        )
    }

    private fun parseFieldAccessor(index: Int): Result<ParseResult<FieldAccessor>> {
        val nextIndex = takeToken(index, TokenType.DOT).getOrElse { return Result.failure(it) }
        val identifierParseResult = parseIdentifier(nextIndex).getOrElse { return Result.failure(it) }
        return Result.success(
            ParseResult(
                nextIndex = identifierParseResult.nextIndex,
                result = FieldAccessor(identifier = identifierParseResult.result)
            )
        )
    }

    private fun parseArrayAccessor(index: Int): Result<ParseResult<ArrayAccessor>> {
        var nextIndex = takeToken(index, TokenType.LBRACKET).getOrElse { return Result.failure(it) }
        val expressionParseResult = parseExpression(nextIndex).getOrElse { return Result.failure(it) }
        nextIndex = takeToken(index, TokenType.RBRACKET).getOrElse { return Result.failure(it) }
        return Result.success(
            ParseResult(
                nextIndex = nextIndex,
                result = ArrayAccessor(expression = expressionParseResult.result),
            )
        )
    }

    // STUB AREA:
    private fun parseProgram(index: Int): Result<ParseResult<Program>> {
        TODO()
    }

    private fun parseDeclaration(index: Int): Result<ParseResult<Declaration>> {
        TODO()
    }

    private fun parseSimpleDeclaration(index: Int): Result<ParseResult<SimpleDeclaration>> {
        TODO()
    }

    private fun parseVariableDeclaration(index: Int): Result<ParseResult<VariableDeclaration>> {
        TODO()
    }

    private fun parseVariableDeclarationWithType(index: Int): Result<ParseResult<VariableDeclarationWithType>> {
        TODO()
    }

    private fun parseVariableDeclarationNoType(index: Int): Result<ParseResult<VariableDeclarationNoType>> {
        TODO()
    }

    private fun parseRoutineDeclaration(index: Int): Result<ParseResult<RoutineDeclaration>> {
        TODO()
    }

    private fun parseRoutineHeader(index: Int): Result<ParseResult<RoutineHeader>> {
        TODO()
    }

    private fun parseRoutineBody(index: Int): Result<ParseResult<RoutineBody>> {
        TODO()
    }

    private fun parseFullRoutineBody(index: Int): Result<ParseResult<FullRoutineBody>> {
        TODO()
    }

    private fun parseSingleExpressionBody(index: Int): Result<ParseResult<SingleExpressionBody>> {
        TODO()
    }

    private fun parseParameters(index: Int): Result<ParseResult<Parameters>> {
        TODO()
    }

    private fun parseParameterDeclaration(index: Int): Result<ParseResult<ParameterDeclaration>> {
        TODO()
    }

    private fun parseStatement(index: Int): Result<ParseResult<Statement>> {
        TODO()
    }

    private fun parseAssignment(index: Int): Result<ParseResult<Assignment>> {
        TODO()
    }

    private fun parseRoutineCall(index: Int): Result<ParseResult<RoutineCall>> {
        TODO()
    }

    private fun parseRoutineCallArgument(index: Int): Result<ParseResult<RoutineCallArgument>> {
        TODO()
    }

    private fun parseWhileLoop(index: Int): Result<ParseResult<WhileLoop>> {
        TODO()
    }

    private fun parseForLoop(index: Int): Result<ParseResult<ForLoop>> {
        TODO()
    }

    private fun parseRange(index: Int): Result<ParseResult<Range>> {
        TODO()
    }

    private fun parseIfStatement(index: Int): Result<ParseResult<IfStatement>> {
        TODO()
    }

    private fun parsePrintStatement(index: Int): Result<ParseResult<PrintStatement>> {
        TODO()
    }

    private fun parseType(index: Int): Result<ParseResult<Type>> {
        TODO()
    }

    private fun parsePrimitiveType(index: Int): Result<ParseResult<PrimitiveType>> {
        TODO()
    }

    private fun parseDeclaredType(index: Int): Result<ParseResult<DeclaredType>> {
        TODO()
    }

    private fun parseUserType(index: Int): Result<ParseResult<UserType>> {
        TODO()
    }

    private fun parseArrayType(index: Int): Result<ParseResult<ArrayType>> {
        TODO()
    }

    private fun parseRecordType(index: Int): Result<ParseResult<RecordType>> {
        TODO()
    }

    private data class ParseResult<out T>(
        val nextIndex: Int,
        val result: T,
    ) {
        fun wrapToResult() = Result.success(this)
    }
}