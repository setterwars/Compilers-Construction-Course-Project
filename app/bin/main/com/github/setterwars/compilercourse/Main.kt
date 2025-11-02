package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser
import java.io.StringReader

fun main(args: Array<String>) {
    val expressionSamples = arrayOf(
        "42",
        "-17",
        "+3.14",
        "not\n 0",
        "value + 5",
        "(x \n-\n 2) * y + \n3",
        "+arr[index + 1] % (-limit - not 0)",
        "record4.field1 xor -10 >= arr[idx].inner.value * +7",
        "count + offset < limit - 1",
        "true and false xor not 0 = 0",
        "(((matrix[i + 1].rows[j - 2].value * (vector[k] + 10))\n\n\n / +3 % not 0) + record.field1 - arr2[idx].inner.value) >= (+5 - shifts[p].delta) and (data[limit - 1].next.ptr.value + totals[section].count % not 0) <= (results[0].score - not 0) xor (flags[index] = true) or (((history[current].entries[last].metric - history[current].entries[last - 1].metric) / (buffers[offset].capacity % 3)) /= (not 0 + cache[h].slots[g]))",
        """
            
            not x.field1[+3 * (y[2].z - -4.5)] 
            * (+7 % -2 / 3.14) 
            + ((true and false) xor (false or true)) 
            - ((+5 <= -6) and (7 > 8 or 9 >= 10 xor 11 < 12))
            and ((+a[1].b.c * 2.0 - (-x2 / 3 % +4)) 
            or ((true) xor (false and (z1 + z2 - z3))))
        """.trimIndent(),
    )

    val lexer = Lexer(StringReader(expressionSamples[11]))
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = lexer.nextToken()
        tokens.add(token)
        if (token.tokenType == TokenType.EOF) break
    }
    println(tokens.map { it.tokenType })
    val parser = Parser(tokens)
    val expression = parser.parse()
    return
}