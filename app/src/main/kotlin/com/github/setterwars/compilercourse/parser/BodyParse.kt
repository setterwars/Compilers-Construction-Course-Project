package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.parser.Parser.ParseResult
import com.github.setterwars.compilercourse.parser.nodes.Body
import com.github.setterwars.compilercourse.parser.nodes.BodyElement

internal fun Parser.parseBody(index: Int): Result<ParseResult<Body>> {
    val bodyElementsParseResult = parseZeroOrMoreTimes(
        index = index,
        parseFunction = { i ->
            parseBodyElement(i)
        }
    )
    return ParseResult(
        nextIndex = bodyElementsParseResult.lastOrNull()?.nextIndex ?: index,
        result = Body(
            bodyElements = bodyElementsParseResult.map { it.result }
        )
    ).wrapToResult()
}

internal fun Parser.parseBodyElement(index: Int): Result<ParseResult<BodyElement>> {
    return combineParseFunctions(
        index = index,
        parseParts = listOf(
            Parser.ParsePart(
                listOf(
                    ::parseSimpleDeclaration,
                    ::parseStatement
                ),
                false
            )
        )
    ) { nodes ->
        match<BodyElement, BodyElement>(nodes) { it }
    }
}
