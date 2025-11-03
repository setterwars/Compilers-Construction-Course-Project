package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.parser.Parser.ParseResult
import com.github.setterwars.compilercourse.parser.nodes.FullRoutineBody
import com.github.setterwars.compilercourse.parser.nodes.ParameterDeclaration
import com.github.setterwars.compilercourse.parser.nodes.Parameters
import com.github.setterwars.compilercourse.parser.nodes.RoutineBody
import com.github.setterwars.compilercourse.parser.nodes.RoutineDeclaration
import com.github.setterwars.compilercourse.parser.nodes.RoutineHeader
import com.github.setterwars.compilercourse.parser.nodes.SingleExpressionBody


internal fun Parser.parseRoutineDeclaration(index: Int): Result<ParseResult<RoutineDeclaration>> {
    return Result.failure(Exception())
}

internal fun Parser.parseRoutineHeader(index: Int): Result<ParseResult<RoutineHeader>> {
    TODO()
}

internal fun Parser.parseRoutineBody(index: Int): Result<ParseResult<RoutineBody>> {
    TODO()
}

internal fun Parser.parseFullRoutineBody(index: Int): Result<ParseResult<FullRoutineBody>> {
    TODO()
}

internal fun Parser.parseSingleExpressionBody(index: Int): Result<ParseResult<SingleExpressionBody>> {
    TODO()
}

internal fun Parser.parseParameters(index: Int): Result<ParseResult<Parameters>> {
    TODO()
}

internal fun Parser.parseParameterDeclaration(index: Int): Result<ParseResult<ParameterDeclaration>> {
    TODO()
}
