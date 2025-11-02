package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.nodes.Assignment
import com.github.setterwars.compilercourse.parser.nodes.Expression
import com.github.setterwars.compilercourse.parser.nodes.ForLoop
import com.github.setterwars.compilercourse.parser.nodes.FullRoutineBody
import com.github.setterwars.compilercourse.parser.nodes.IfStatement
import com.github.setterwars.compilercourse.parser.nodes.ParameterDeclaration
import com.github.setterwars.compilercourse.parser.nodes.Parameters
import com.github.setterwars.compilercourse.parser.nodes.PrintStatement
import com.github.setterwars.compilercourse.parser.nodes.Program
import com.github.setterwars.compilercourse.parser.nodes.Range
import com.github.setterwars.compilercourse.parser.nodes.RoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineCall
import com.github.setterwars.compilercourse.parser.nodes.RoutineCallArgument
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.RoutineHeader
import com.github.setterwars.compilercourse.parser.nodes.SingleExpressionBody
import com.github.setterwars.compilercourse.parser.nodes.Statement
import com.github.setterwars.compilercourse.parser.nodes.WhileLoop
import kotlin.random.Random

class Parser(internal val tokens: List<Token>) {
    fun parse(): Program? {
        return parseProgram(0).getOrNull()?.result
    }

    internal fun getToken(index: Int): Token? {
        return tokens.getOrNull(index)
    }

    internal data class ParseResult<out T>(
        val nextIndex: Int,
        val result: T,
    ) {
        fun wrapToResult() = Result.success(this)
    }

    internal class WrongTokenTypeException(
        internal val token: Token?,
        internal val expectedTypes: List<TokenType>,
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

    internal fun <T> parseAnyTokenOf(
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
        return Result.failure(WrongTokenTypeException(token = getToken(currentIndex), tokenTypes))
    }

    internal data class ParsePart(
        val alternatingParseFunctions: List<(Int) -> Result<ParseResult<Any>>>,
        val optional: Boolean = false,
    ) {
        constructor(
            parseFunction: (Int) -> Result<ParseResult<Any>>,
            optional: Boolean = false
        ) : this(listOf(parseFunction), optional)
    }

    internal fun <T> combineParseFunctions(
        index: Int,
        parseParts: List<ParsePart>,
        transform: (List<Any>) -> T?,
    ): Result<ParseResult<T>> {
        val parsePartsStates = MutableList(size = parseParts.size) { -1 }
        val parsePartsResults = mutableListOf<Pair<Int, ParseResult<Any>>>() // pair (parse part index, result)
        var lastFailure = Result.failure<ParseResult<T>>(Exception())

        //  This is basically recursive bruteforce written with stack and while
        val id = Random.nextInt()
        var parsePartsIndex = 0
        while (true) {
            if (parsePartsIndex == parseParts.size) {
                if (parsePartsResults.isNotEmpty()) {
                    transform(parsePartsResults.map { it.second.result })?.let { finalResult ->
                        return Result.success(
                            ParseResult(
                                nextIndex = parsePartsResults.last().second.nextIndex,
                                result = finalResult,
                            )
                        )
                    }
                }
                parsePartsIndex--
                if (parsePartsResults.last().first == parsePartsIndex) {
                    parsePartsResults.removeLast()
                }
            }

            while (
                parsePartsIndex >= 0 &&
                parsePartsStates[parsePartsIndex] == parseParts[parsePartsIndex].alternatingParseFunctions.size - 1
            ) {
                parsePartsStates[parsePartsIndex] = -1
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
                if (parseParts[parsePartsIndex].optional) {
                    parsePartsIndex++
                } else {
                    lastFailure = Result.failure(it)
                }
            }
        }
        return lastFailure
    }

    internal fun takeToken(index: Int, tokenType: TokenType): Result<Int> {
        val startIndex = skipNewLines(index)
        val token = getToken(startIndex)
        if (token?.tokenType == tokenType) {
            return Result.success(startIndex + 1)
        }
        return Result.failure(WrongTokenTypeException(token, tokenType))
    }

    internal fun <T> parseZeroOrMoreTimes(
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

    internal inline fun <reified T, reified R1> match(nodes: List<Any>, transform: (R1) -> T): T? {
        if (nodes.size != 1) return null
        val node0 = nodes[0]
        return if (node0 is R1) transform(node0) else null
    }

    internal inline fun <reified T, reified R1, reified R2> match(nodes: List<Any>, transform: (R1, R2) -> T): T? {
        if (nodes.size != 2) return null
        val node0 = nodes[0]
        val node1 = nodes[1]
        return if (node0 is R1 && node1 is R2) transform(node0, node1) else null
    }


    internal fun <T> parseToken(
        index: Int,
        tokenType: TokenType,
        transform: (Token) -> T?,
    ): Result<ParseResult<T>> {
        return parseAnyTokenOf(index, listOf(tokenType), transform)
    }

    internal fun skipNewLines(index: Int): Int {
        var currentIndex = index
        while (currentIndex < tokens.size && getToken(currentIndex)?.tokenType == TokenType.NEW_LINE) {
            currentIndex++
        }
        return currentIndex
    }

    internal fun parseRoutineDeclaration(index: Int): Result<ParseResult<RoutineDeclaration>> {
        return Result.failure(Exception())
    }

    internal fun parseRoutineHeader(index: Int): Result<ParseResult<RoutineHeader>> {
        TODO()
    }

    internal fun parseRoutineBody(index: Int): Result<ParseResult<RoutineBody>> {
        TODO()
    }

    internal fun parseFullRoutineBody(index: Int): Result<ParseResult<FullRoutineBody>> {
        TODO()
    }

    internal fun parseSingleExpressionBody(index: Int): Result<ParseResult<SingleExpressionBody>> {
        TODO()
    }

    internal fun parseParameters(index: Int): Result<ParseResult<Parameters>> {
        TODO()
    }

    internal fun parseParameterDeclaration(index: Int): Result<ParseResult<ParameterDeclaration>> {
        TODO()
    }

    internal fun parseStatement(index: Int): Result<ParseResult<Statement>> {
        TODO()
    }

    internal fun parseAssignment(index: Int): Result<ParseResult<Assignment>> {
        TODO()
    }

    internal fun parseRoutineCall(index: Int): Result<ParseResult<RoutineCall>> {
        return Result.failure(NotImplementedError())
    }

    internal fun parseRoutineCallArgument(index: Int): Result<ParseResult<RoutineCallArgument>> {
        TODO()
    }

    internal fun parseWhileLoop(index: Int): Result<ParseResult<WhileLoop>> {
        TODO()
    }

    internal fun parseForLoop(index: Int): Result<ParseResult<ForLoop>> {
        TODO()
    }

    internal fun parseRange(index: Int): Result<ParseResult<Range>> {
        TODO()
    }

    internal fun parseIfStatement(index: Int): Result<ParseResult<IfStatement>> {
        TODO()
    }

    internal fun parsePrintStatement(index: Int): Result<ParseResult<PrintStatement>> {
        TODO()
    }

}