package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser
import com.github.setterwars.compilercourse.parser.parseBody
import java.io.StringReader

fun main(args: Array<String>) {
    val statementOnlyTests = arrayOf(
        // 10) COMPLEX COMPOUND — combine loops, calls, prints, assignments, ifs
        """
s := 0
n := 4
if n > 0 then
  s := 1
end
for k in 1 .. n loop
  s := s + k
  step(k)
end
while s < 20 loop
  s := s + 3
  tick()
  if s >= 15 then
    print s, probe(s)
  end
end
print s, finalize(s)
  """.trimIndent(),
    )


    val lexer = Lexer(StringReader(statementOnlyTests[0]))
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = lexer.nextToken()
        tokens.add(token)
        if (token.tokenType == TokenType.EOF) break
    }
    println(tokens.map { it.tokenType })
    val parser = Parser(tokens)
    val statements = parser.parseBody(0)
    return
}