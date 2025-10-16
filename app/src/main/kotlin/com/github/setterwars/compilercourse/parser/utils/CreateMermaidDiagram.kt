package com.github.setterwars.compilercourse.parser.utils

import com.github.setterwars.compilercourse.parser.*
import java.io.Writer
import kotlin.reflect.KClass

val semanticTerminalClasses: Set<KClass<out Terminal>> = setOf(
    TIdentifier::class,
    TIntLiteral::class,
    TRealLiteral::class,
    TFalse::class,
    TTrue::class,
    TAnd::class,
    TOr::class,
    TXor::class,
    TNot::class,
    TLt::class,
    TLe::class,
    TGt::class,
    TGe::class,
    TEq::class,
    TNe::class,
    TPlus::class,
    TMinus::class,
    TStar::class,
    TSlash::class,
    TPercent::class,
    TAssign::class,
    TRangeDots::class,
    TIntegerKw::class,
    TRealKw::class,
    TBooleanKw::class,
    TRecord::class,
    TArray::class,
    TPrint::class,
    TIn::class,
    TReverse::class
)

fun createMermaidDiagram(writer: Writer, symbol: Symbol, truncate: Boolean = false, maxDepth: Int = -1) {
    with(writer) {
        write("```mermaid\n")
        write("graph TD\n")
        write(
            generateSequences(
                symbol,
                currentDepth = 0,
                truncate = truncate,
                maxDepth = maxDepth
            ).first.joinToString("\n") { "   $it" })
        write("\n```")
    }
}


private fun generateSequences(
    symbol: Symbol,
    currentDepth: Int = 0,
    truncate: Boolean,
    maxDepth: Int = -1
): Pair<List<String>, Symbol> {
    return when (symbol) {
        is Terminal -> (emptyList<String>() to symbol)
        is NonTerminal -> {
            if (currentDepth == maxDepth) {
                emptyList<String>() to symbol
            } else if (!truncate || symbol.children.size > 1 || symbol is ProgramNode) {
                val list = buildList {
                    for (child in symbol.children) {
                        val (generated, nextChild) = generateSequences(child, currentDepth + 1, truncate, maxDepth)
                        if (nextChild is NonTerminal || (nextChild is Terminal && (!truncate || nextChild::class in semanticTerminalClasses))) {
                            add(connection(symbol, nextChild))
                        }
                        addAll(generated)
                    }
                }
                list to symbol
            } else if (symbol.children.isNotEmpty()) {
                generateSequences(symbol.children[0], currentDepth + 1, truncate, maxDepth)
            } else {
                emptyList<String>() to symbol
            }
        }
    }
}

private fun connection(symbol1: Symbol, symbol2: Symbol): String {
    return "${symbol1.hashCode()}[${symbol1::class.simpleName}] --> ${symbol2.hashCode()}[${symbol2::class.simpleName}]"
}