package com.github.setterwars.compilercourse

import kotlin.toString

class Parser(private val tokens: List<Token>) {
    private var pos = 0

    private fun getNextToken(): Token? = tokens.getOrNull(pos)
    private fun advance(): Token? = tokens.getOrNull(pos++)

    private fun expect(kind: TokenType): Token {
        val token = advance() ?: error("Unexpected end of input, expected $kind")
        if (token.tokenType != kind) error("Expected $kind but got ${token.tokenType} at ${token.tokenType}")
        return token
    }

    fun tokensToAst(): ProgramNode {
        val declarations = mutableListOf<ASTNode>()
        while (true) {
            val nextToken = getNextToken()
            if (nextToken == null || nextToken.tokenType == TokenType.EOF) break
            declarations.add(parseDeclaration())
            while (getNextToken()?.tokenType == TokenType.NEW_LINE || getNextToken()?.tokenType == TokenType.SEMICOLON) {
                advance()
            }
        }
        if (getNextToken()?.tokenType == TokenType.EOF) {
            expect(TokenType.EOF)
        }
        return ProgramNode(declarations)
    }

    private fun parseDeclaration(): ASTNode =
        when (getNextToken()?.tokenType) {
            TokenType.VAR -> parseVarDecl()
            TokenType.TYPE -> parseTypeDecl()
            TokenType.ROUTINE -> parseRoutineDecl()
            TokenType.IF -> parseIf()
            TokenType.WHILE -> parseWhile()
            TokenType.FOR -> parseFor()
            TokenType.PRINT -> parsePrint()
            TokenType.IDENTIFIER -> parseAssignOrCall()
            TokenType.DOT -> { advance(); parseDeclaration(); }
            TokenType.NEW_LINE, TokenType.SEMICOLON, TokenType.COMMA -> { advance(); parseDeclaration(); }
            TokenType.INT_LITERAL, TokenType.REAL_LITERAL, TokenType.TRUE, TokenType.FALSE -> { advance(); parseDeclaration(); }
            TokenType.EOF -> parseEOF()
            else -> error("Unexpected token ${getNextToken()} at top-level")
        }

    private fun parseEOF(): ASTNode {
        expect(TokenType.EOF)
        return EOFNode()
    }

    private fun parseVarDecl(): VarDeclNode {
        expect(TokenType.VAR)
        val name = expect(TokenType.IDENTIFIER).lexeme
        var type: TypeNode? = null
        var init: ExprNode? = null
        if (getNextToken()?.tokenType == TokenType.COLON) {
            advance()
            type = parseType()
        }
        if (getNextToken()?.tokenType == TokenType.IS) {
            advance()
            init = parseExpression()
        }
        return VarDeclNode(name, type, init)
    }

    private fun parseTypeDecl(): TypeDeclNode {
        expect(TokenType.TYPE)
        val name = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.IS)
        val baseType = parseType()
        return TypeDeclNode(name, baseType)
    }

    private fun parseRoutineDecl(): RoutineDeclNode {
        expect(TokenType.ROUTINE)
        val name = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.LPAREN)
        val params = mutableListOf<ParamNode>()
        if (getNextToken()?.tokenType != TokenType.RPAREN) {
            params.add(parseParam())
            while (getNextToken()?.tokenType == TokenType.COMMA) {
                advance()
                params.add(parseParam())
            }
        }
        expect(TokenType.RPAREN)
        var returnType: TypeNode? = null
        if (getNextToken()?.tokenType == TokenType.COLON) {
            advance()
            returnType = parseType()
        }
        if (getNextToken()?.tokenType == TokenType.ARROW) {
            advance()
            val exprBody = parseExpression()
            return RoutineDeclNode(name, params, returnType, BodyNode(listOf(ReturnNode(exprBody))))
        }
        val body = if (getNextToken()?.tokenType == TokenType.IS) {
            parseRoutineBody()
        } else {
            null
        }
        return RoutineDeclNode(name, params, returnType, body)
    }

    private fun parseParam(): ParamNode {
        val name = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.COLON)
        val type = parseType()
        return ParamNode(name, type)
    }

    private fun parseBlockBody(terminators: Set<TokenType>): BodyNode {
        while (getNextToken()?.tokenType == TokenType.NEW_LINE || getNextToken()?.tokenType == TokenType.SEMICOLON) {
            advance()
        }
        val items = mutableListOf<ASTNode>()
        while (true) {
            val next = getNextToken()?.tokenType ?: break
            if (next in terminators) break
            if (next == TokenType.NEW_LINE || next == TokenType.SEMICOLON) {
                advance()
                continue
            }
            items.add(parseStatementOrDecl())
            while (getNextToken()?.tokenType == TokenType.NEW_LINE || getNextToken()?.tokenType == TokenType.SEMICOLON) {
                advance()
            }
        }
        return BodyNode(items)
    }

    private fun parseRoutineBody(): BodyNode {
        while (getNextToken()?.tokenType == TokenType.NEW_LINE) advance()
        expect(TokenType.IS)
        val items = parseBlockBody(setOf(TokenType.END))
        expect(TokenType.END)
        return items
    }

    private fun parseStatementOrDecl(): ASTNode =
        when (getNextToken()?.tokenType) {
            TokenType.VAR -> parseVarDecl()
            TokenType.TYPE -> parseTypeDecl()
            TokenType.IDENTIFIER -> parseAssignOrCall()
            TokenType.WHILE -> parseWhile()
            TokenType.FOR -> parseFor()
            TokenType.IF -> parseIf()
            TokenType.PRINT -> parsePrint()
            TokenType.NEW_LINE -> parseNewLineNode()
            TokenType.EOF -> parseEOF()
            TokenType.RETURN -> parseReturnNode()
            // Support expression statements
            TokenType.INT_LITERAL, TokenType.REAL_LITERAL, TokenType.TRUE, TokenType.FALSE,
            TokenType.LPAREN, TokenType.PLUS, TokenType.MINUS -> {
                ExprStmtNode(parseExpression())
            }
            else -> error("Unexpected token ${getNextToken()} in body")
        }

    private fun parseReturnNode(): ASTNode {
        expect(TokenType.RETURN)
        val starters = setOf(
            TokenType.IDENTIFIER,
            TokenType.INT_LITERAL,
            TokenType.REAL_LITERAL,
            TokenType.TRUE,
            TokenType.FALSE,
            TokenType.LPAREN,
            TokenType.PLUS,
            TokenType.MINUS
        )
        return if (getNextToken()?.tokenType in starters) {
            ReturnNode(parseExpression())
        } else {
            ReturnNode()
        }
    }

    private fun parseAssignOrCall(): ASTNode {
        val name = expect(TokenType.IDENTIFIER).lexeme
        val selectors = mutableListOf<SelectorNode>()
        // Parse selectors (array indexing, field access)
        while (true) {
            when (getNextToken()?.tokenType) {
                TokenType.LBRACKET -> {
                    advance()
                    val indexExpr = parseExpression()
                    expect(TokenType.RBRACKET)
                    selectors.add(IndexAccessNode(indexExpr))
                }
                TokenType.DOT -> {
                    advance()
                    val field = expect(TokenType.IDENTIFIER).lexeme
                    selectors.add(FieldAccessNode(field))
                }
                else -> break
            }
        }
        return if (getNextToken()?.tokenType == TokenType.ASSIGN) {
            advance()
            val value = parseExpression()
            AssignNode(ModifiablePrimaryNode(name, selectors), value)
        } else {
            val args = mutableListOf<ExprNode>()
            if (getNextToken()?.tokenType == TokenType.LPAREN) {
                advance()
                if (getNextToken()?.tokenType != TokenType.RPAREN) {
                    args.add(parseExpression())
                    while (getNextToken()?.tokenType == TokenType.COMMA) {
                        advance()
                        args.add(parseExpression())
                    }
                }
                expect(TokenType.RPAREN)
            }
            CallNode(name, args)
        }
    }

    private fun parseWhile(): WhileNode {
        expect(TokenType.WHILE)
        val cond = parseExpression()
        expect(TokenType.LOOP)
        val body = parseBlockBody(setOf(TokenType.END))
        expect(TokenType.END)
        return WhileNode(cond, body)
    }

    private fun parseFor(): ForNode {
        expect(TokenType.FOR)
        val varName = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.IN)
        val start = parseExpression()
        var end: ExprNode? = null
        when (getNextToken()?.tokenType) {
            TokenType.RANGE -> { advance(); end = parseExpression() }
            TokenType.DOT -> {
                advance()
                if (getNextToken()?.tokenType == TokenType.DOT) {
                    advance()
                    end = parseExpression()
                }
            }
            else -> { /* No action needed for unexpected tokens */ }
        }
        val range = RangeNode(start, end)
        val reverse = if (getNextToken()?.tokenType == TokenType.REVERSE) { advance(); true } else false
        expect(TokenType.LOOP)
        val body = parseBlockBody(setOf(TokenType.END))
        expect(TokenType.END)
        return ForNode(varName, range, reverse, body)
    }

    private fun parseIf(): IfNode {
        expect(TokenType.IF)
        val cond = parseExpression()
        expect(TokenType.THEN)
        val thenBody = parseBlockBody(setOf(TokenType.ELSE, TokenType.END))
        var elseBody: BodyNode? = null
        if (getNextToken()?.tokenType == TokenType.ELSE) {
            advance()
            elseBody = parseBlockBody(setOf(TokenType.END))
        }
        expect(TokenType.END)
        return IfNode(cond, thenBody, elseBody)
    }

    private fun parsePrint(): PrintNode {
        expect(TokenType.PRINT)
        val args = mutableListOf<ExprNode>()
        args.add(parseExpression())
        while (getNextToken()?.tokenType == TokenType.COMMA) {
            advance()
            args.add(parseExpression())
        }
        return PrintNode(args)
    }

    private fun parseType(): TypeNode {
        val token = advance() ?: error("Unexpected end of input in type")
        return when (token.tokenType) {
            TokenType.INTEGER -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.INTEGER)
            TokenType.REAL -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.REAL)
            TokenType.ARRAY -> {
                expect(TokenType.LBRACKET)
                val size = parseExpression()
                expect(TokenType.RBRACKET)
                val elementType = parseType()
                ArrayTypeNode(size, elementType)
            }
            TokenType.BOOLEAN -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.BOOLEAN)
            TokenType.RECORD -> {
                val fields = mutableListOf<VarDeclNode>()
                while (true) {
                    // Skip any newlines/semicolons before next field or END
                    while (getNextToken()?.tokenType == TokenType.NEW_LINE || getNextToken()?.tokenType == TokenType.SEMICOLON) {
                        advance()
                    }
                    val next = getNextToken()?.tokenType
                    if (next == TokenType.VAR) {
                        fields.add(parseVarDecl())
                    } else if (next == TokenType.END) {
                        advance()
                        break
                    } else if (next == null) {
                        error("Unexpected end of input in record type")
                    } else {
                        error("Unexpected token $next in record type, expected VAR or END")
                    }
                }
                RecordTypeNode(fields)
            }
            TokenType.IDENTIFIER -> NamedTypeNode(token.lexeme)
            else -> error("Unexpected token $token in type")
        }
    }


    private fun parseExpression(): ExprNode {
        return parseOr()
    }

    private fun parseOr(): ExprNode {
        var expr = parseAnd()
        while (getNextToken()?.tokenType == TokenType.OR) {
            advance()
            val right = parseAnd()
            expr = BinaryOpNode(expr, "or", right)
        }
        return expr
    }

    private fun parseAnd(): ExprNode {
        var expr = parseEquality()
        while (getNextToken()?.tokenType == TokenType.AND) {
            advance()
            val right = parseEquality()
            expr = BinaryOpNode(expr, "and", right)
        }
        return expr
    }

    private fun parseEquality(): ExprNode {
        var expr = parseRelational()
        while (getNextToken()?.tokenType in listOf(TokenType.LE, TokenType.LT, TokenType.GE, TokenType.GT, TokenType.EQ)) {
            val op = advance()!!.tokenType.toString()
            val right = parseRelational()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseRelational(): ExprNode {
        var expr = parseAdditive()
        while (getNextToken()?.tokenType in listOf(TokenType.PLUS, TokenType.MINUS)) {
            val op = advance()!!.tokenType.toString()
            val right = parseAdditive()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    // Add multiplicative precedence for STAR and SLASH
    private fun parseAdditive(): ExprNode {
        var expr = parseMultiplicative()
        while (getNextToken()?.tokenType in listOf(TokenType.PLUS, TokenType.MINUS)) {
            val op = advance()!!.tokenType.toString()
            val right = parseMultiplicative()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseMultiplicative(): ExprNode {
        var expr = parseUnary()
        while (getNextToken()?.tokenType in listOf(TokenType.STAR, TokenType.SLASH)) {
            val op = advance()!!.tokenType.toString()
            val right = parseUnary()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parseUnary(): ExprNode {
        if (getNextToken()?.tokenType == TokenType.NOT) {
            advance()
            val expr = parseUnary()
            return UnaryOpNode("not", expr)
        }
        return parsePrimary()
    }

    private fun parsePrimary(): ExprNode {
        val token = advance() ?: error("Unexpected end of input in primary")
        return when (token.tokenType) {
            TokenType.INT_LITERAL -> LiteralNode(token.lexeme.toInt())
            TokenType.REAL_LITERAL -> LiteralNode(token.lexeme.toDouble())
            TokenType.IDENTIFIER -> {
                val name = token.lexeme
                if (getNextToken()?.tokenType == TokenType.LPAREN) {
                    advance()
                    val args = mutableListOf<ExprNode>()
                    if (getNextToken()?.tokenType != TokenType.RPAREN) {
                        args.add(parseExpression())
                        while (getNextToken()?.tokenType == TokenType.COMMA) {
                            advance()
                            args.add(parseExpression())
                        }
                    }
                    expect(TokenType.RPAREN)
                    CallExprNode(name, args)
                } else {
                    VarRefNode(name)
                }
            }
            TokenType.TRUE -> LiteralNode(true)
            TokenType.FALSE -> LiteralNode(false)
            TokenType.LPAREN -> {
                val inner = parseExpression()
                expect(TokenType.RPAREN)
                inner
            }
            TokenType.PLUS, TokenType.MINUS -> {
                val op = token.tokenType.toString()
                val inner = parsePrimary()
                UnaryOpNode(op, inner)
            }
            else -> error("Unexpected token $token in primary")
        }
    }

    private fun parseNewLineNode(): NewLineNode {
        expect(TokenType.NEW_LINE)
        return NewLineNode()
    }
}
