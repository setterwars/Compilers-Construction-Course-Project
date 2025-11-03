package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser
import com.github.setterwars.compilercourse.parser.parseBody
import java.io.StringReader

fun main(args: Array<String>) {
    val routineAndProgramTests = arrayOf(
        // --- Routine-focused: headers & bodies (minimal other features) ---
        // 1) No params, no return type, empty body
        """
routine r0() is
end
  """.trimIndent(),

        // 2) Multiple params with primitives, return type, expression body
        """
routine r1(a: integer, b: real, c: boolean): integer => a
  """.trimIndent(),

        // 3) Return real with signed literal
        """
routine r2(): real => +3.14
  """.trimIndent(),

        // 4) Identifier type in params/return (single simple declaration to enable Identifier type)
        """
type Alias is integer
routine r3(p: Alias, q: Alias): Alias => p
  """.trimIndent(),

        // 5) Empty parameter list with extra whitespace, boolean return via expression body
        """
routine r4(   ): boolean => true
  """.trimIndent(),

        // 6) Block body with a couple of simple statements
        """
routine r5(x: integer): integer is
  x := x + 1
  print x
end
  """.trimIndent(),

        // 7) Nested if in a block body (no return type)
        """
routine r6(a: integer, b: integer) is
  if a > 0 then
    if b > 0 then
      print a, b
    else
      b := 1
    end
  end
end
  """.trimIndent(),

        // 8) Relational/boolean expression in concise body
        """
routine r7(a: integer, b: integer): boolean => (a < b) or (a = b)
  """.trimIndent(),

        // --- Large Program 1: geometry utilities (ONLY simple decls + routines) ---
        """
type Point is record
  var x: real
  var y: real
end
type Segment is record
  var a: Point
  var b: Point
end

var origin: Point

routine makePoint(x: real, y: real): Point is
  origin.x := 0.0
  origin.y := 0.0
  print origin.x, origin.y
end

routine length2(p: Point): real => (p.x * p.x) + (p.y * p.y)

routine midpoint(s: Segment): Point is
  # placeholder body; parser just needs the structure
end

routine translate(p: Point, dx: real, dy: real): Point is
  print p.x + dx, p.y + dy
end

routine mainGeom() is
  print origin.x, origin.y
end
  """.trimIndent(),

        // --- Large Program 2: image processing scaffolding (arrays, records, loops) ---
        """
var width: integer is 4
var height: integer is 3

type Pixel is record
  var r: integer
  var g: integer
  var b: integer
end

type Image is array[width * height] Pixel
var buffer: Image
var globalCount: integer

routine clear(img: Image) is
  i := 0
  while i < width * height loop
    img[i].r := 0
    img[i].g := 0
    img[i].b := 0
    i := i + 1
  end
end

routine fillGradient(img: Image) is
  i := 0
  while i < width * height loop
    img[i].r := i
    img[i].g := i * 2
    img[i].b := img[i].r + img[i].g
    i := i + 1
  end
  print img[0].r, img[0].g, img[0].b
end

routine sumChannel(img: Image, ch: integer): integer is
  s := 0
  i := 0
  while i < width * height loop
    if ch = 0 then
      s := s + img[i].r
    else
      if ch = 1 then
        s := s + img[i].g
      else
        s := s + img[i].b
      end
    end
    i := i + 1
  end
  print s
end

routine pipeline() is
  clear(buffer)
  fillGradient(buffer)
  globalCount := sumChannel(buffer, 0)
  print globalCount
end
  """.trimIndent(),

        // --- Large Program 3: assorted routine headers & concise bodies ---
        """
type Id is integer
type Flag is boolean
var threshold: integer is 5
var enabled: boolean is true

routine id(x: Id): Id => x

routine negate(b: Flag): Flag => not b

routine step(n: integer): integer is
  if n > threshold then
    print n
  else
    print threshold
  end
end

routine sumTo(n: integer): integer is
  acc := 0
  for i in n loop
    acc := acc + i
  end
  print acc
end

routine choose(a: integer, b: integer, takeA: boolean): integer => a  # header variety

routine status(): boolean => enabled

routine configure(n: integer, e: boolean) is
  threshold := n
  enabled := e
  print threshold, enabled
end
  """.trimIndent()
    )


    val lexer = Lexer(StringReader(routineAndProgramTests[0]))
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = lexer.nextToken()
        tokens.add(token)
        if (token.tokenType == TokenType.EOF) break
    }
    println(tokens.map { it.tokenType })
    val parser = Parser(tokens)
    val program = parser.parse()
    return
}