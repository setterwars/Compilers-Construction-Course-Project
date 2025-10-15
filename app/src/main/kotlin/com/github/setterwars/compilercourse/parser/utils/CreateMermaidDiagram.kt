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

fun createMermaidDiagram(writer: Writer, symbol: Symbol, truncate: Boolean = false) {
    with(writer) {
        write("```mermaid\n")
        write("graph TD\n")
        if (truncate) {
            write(generateSequencesTruncated(symbol).first.joinToString("\n") { "   $it" })
        } else {
            write(generateSequences(symbol).joinToString("\n") { "   $it" })
        }
        write("\n```")
    }
}

private fun generateSequences(symbol: Symbol): List<String> {
    return when (symbol) {
        is Terminal -> emptyList()
        is NonTerminal -> buildList {
            for (child in symbol.children) {
                add(connection(symbol, child))
                addAll(generateSequences(child))
            }
        }
    }
}

private fun generateSequencesTruncated(symbol: Symbol): Pair<List<String>, Symbol> {
    return when (symbol) {
        is Terminal -> (emptyList<String>() to symbol)
        is NonTerminal -> {
            if (symbol.children.size > 1 || symbol is ProgramNode) {
                val list = buildList {
                    for (child in symbol.children) {
                        val (generated, nextChild) = generateSequencesTruncated(child)
                        if (nextChild is NonTerminal || (nextChild is Terminal && nextChild::class in semanticTerminalClasses)) {
                            add(connection(symbol, nextChild))
                        }
                        addAll(generated)
                    }
                }
                list to symbol
            } else if (symbol.children.isNotEmpty()) {
                generateSequencesTruncated(symbol.children[0])
            } else {
                emptyList<String>() to symbol
            }
        }
    }
}

private fun connection(symbol1: Symbol, symbol2: Symbol): String {
    return "${symbol1.hashCode()}[${symbol1::class.simpleName}] --> ${symbol2.hashCode()}[${symbol2::class.simpleName}]"
}