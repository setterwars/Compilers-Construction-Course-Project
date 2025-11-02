package com.github.setterwars.compilercourse

import com.github.setterwars.compilercourse.lexer.Lexer
import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import com.github.setterwars.compilercourse.parser.Parser
import java.io.StringReader

fun main(args: Array<String>) {
    val comprehensiveParserCases = arrayOf(
        // 1) Primitives, declared type, precedence, signs, boolean ops, relations, modulo
        """
type Alias is integer
var x: integer
var y is -42
var z: boolean is false and (1 + 2 * 3 >= 7) xor true
var r: real is +3.5 / 7
var a: Alias is 5
var b: integer is 10 % 3
  """.trimIndent(),

        // 2) Arrays (unsized/sized/nested), Identifier as Type, size as expressions
        """
var n: integer is 2
var cols: integer is 4
var size: integer is 3 + n
var a1: array[] real
var a2: array[10] integer
var a3: array[size] boolean
var grid: array[2] array[3] boolean
type Row is array[cols] integer
type Table is array[n] Row
var t: Table
  """.trimIndent(),

        // 3) Records (inline + declared) with mixed var forms inside
        """
var point: record var x: real var y: real end
type R is record var i: integer var j is 3 end
var r1: R
var r2: record var on: boolean is true var level: integer end
  """.trimIndent(),

        // 4) Mixed/nested user types: arrays of records and records with arrays
        """
type Pixel is record var r: integer var g: integer var b: integer end
var width: integer is 2
var height: integer is 3
var pixels: array[width*height] Pixel
var map: array[] record var key: integer var value: real end
var complex: record var coords: array[3] real var flags: array[] boolean end
  """.trimIndent(),

        // 5) ModifiablePrimary chaining (field access & indexing) in initializers
        """
type Vec2 is record var x: real var y: real end
type Mat is array[3] array[3] real
type Nested is record var a: record var b: array[10] integer end end
var v: Vec2
var M1: Mat
var nst: Nested
var vx: real is v.x
var cell: real is M1[1][2]
var v2: integer is nst.a.b[3]
var check: boolean is (M1[0][0] < 10) and (vx /= 0.0 or false)
  """.trimIndent()
    )


    val lexer = Lexer(StringReader(comprehensiveParserCases[4]))
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