package com.github.setterwars.compilercourse

class Parser(private val tokens: List<Token>) {
    private var pos = 0

    private fun getNextToken(): Token? = tokens.getOrNull(pos)
    private fun advance(): Token? = tokens.getOrNull(pos++)

    private fun expect(kind: TokenType): Token {
        val token = advance() ?: error("Unexpected end of input, expected $kind")
        if (token.tokenType != kind) error("Expected $kind but got ${token.tokenType} at ${token.tokenType}")
        return token
    }

    // Entry point to parser
    fun tokensToAst(): ProgramNode {
        val declarations = mutableListOf<ASTNode>()
        while (getNextToken()?.tokenType != TokenType.EOF) {
            declarations.add(parseDeclaration())
        }
        expect(TokenType.EOF) // consume EOF once
        return ProgramNode(declarations)
    }


    // Parse one declaration (var, type, or routine)
    private fun parseDeclaration(): ASTNode {
        return when (getNextToken()?.tokenType) {
            TokenType.VAR -> parseVarDecl()
            TokenType.TYPE -> parseTypeDecl()
            TokenType.ROUTINE -> parseRoutineDecl()
            TokenType.IF -> parseIf()
            TokenType.WHILE -> parseWhile()
            TokenType.FOR -> parseFor()
            TokenType.PRINT -> parsePrint()
            TokenType.IDENTIFIER -> parseAssignOrCall()
            TokenType.NEW_LINE -> parseNewLineNode()
            TokenType.EOF -> parseEOF()
            else -> error("Unexpected token ${getNextToken()} at top-level")
        }
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

        val body = parseRoutineBody()

        return RoutineDeclNode(name, params, returnType, body)
    }

    private fun parseParam(): ParamNode {
        val name = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.COLON)
        val type = parseType()
        return ParamNode(name, type)
    }

    private fun parseRoutineBody(): BodyNode {
        expect(TokenType.IS)
        val items = mutableListOf<ASTNode>()
        while (getNextToken()?.tokenType != TokenType.END) {
            items.add(parseStatementOrDecl())
        }
        expect(TokenType.END)
        return BodyNode(items)
    }

    private fun parseStatementOrDecl(): ASTNode {
        return when (getNextToken()?.tokenType) {
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
            else -> error("Unexpected token ${getNextToken()} in body")
        }
    }

    // UPDATED: consume optional expression after RETURN to avoid leaving expression tokens unconsumed
    private fun parseReturnNode(): ASTNode {
        expect(TokenType.RETURN)
        // If next token can start an expression, parse it and discard the result
        when (getNextToken()?.tokenType) {
            TokenType.IDENTIFIER,
            TokenType.INT_LITERAL,
            TokenType.REAL_LITERAL,
            TokenType.TRUE,
            TokenType.FALSE,
            TokenType.LPAREN,
            TokenType.PLUS,
            TokenType.MINUS -> {
                // parseExpression will consume the tokens belonging to the returned expression
                parseExpression()
            }
            else -> {
                // no expression after return (e.g., `return` alone) — that's fine
            }
        }
        return ReturnNode()
    }

    // ===== STATEMENTS =====
    private fun parseAssignOrCall(): ASTNode {
        val name = expect(TokenType.IDENTIFIER).lexeme
        return if (getNextToken()?.tokenType == TokenType.ASSIGN) {
            advance()
            val value = parseExpression()
            AssignNode(ModifiablePrimaryNode(name, emptyList()), value)
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
        val body = parseRoutineBody()
        return WhileNode(cond, body)
    }

    private fun parseFor(): ForNode {
        expect(TokenType.FOR)
        val varName = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.IN)
        val start = parseExpression()
        var end: ExprNode? = null
        if (getNextToken()?.tokenType == TokenType.DOT) {
            advance()
            end = parseExpression()
        }
        val range = RangeNode(start, end)
        val reverse = if (getNextToken()?.tokenType == TokenType.REVERSE) { advance(); true } else false
        expect(TokenType.LOOP)
        val body = parseRoutineBody()
        return ForNode(varName, range, reverse, body)
    }

    private fun parseIf(): IfNode {
        expect(TokenType.IF)
        val cond = parseExpression()
        expect(TokenType.THEN)
        val thenBody = parseRoutineBody()
        var elseBody: BodyNode? = null
        if (getNextToken()?.tokenType == TokenType.ELSE) {
            advance()
            elseBody = parseRoutineBody()
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

    // ===== TYPES =====
    private fun parseType(): TypeNode {
        val token = advance() ?: error("Unexpected end of input in type")
        return when (token.tokenType) {
            TokenType.INTEGER -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.INTEGER)
            TokenType.REAL -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.REAL)
            TokenType.BOOLEAN -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.BOOLEAN)
            else -> error("Unexpected token $token in type")
        }
    }

    // ===== EXPRESSIONS =====
    private fun parseExpression(): ExprNode {
        var expr = parsePrimary()
        while (getNextToken()?.tokenType in listOf(TokenType.PLUS, TokenType.MINUS)) {
            val op = advance()!!.tokenType.toString()
            val right = parsePrimary()
            expr = BinaryOpNode(expr, op, right)
        }
        return expr
    }

    private fun parsePrimary(): ExprNode {
        val token = advance() ?: error("Unexpected end of input in primary")
        return when (token.tokenType) {
            TokenType.INT_LITERAL -> LiteralNode(token.lexeme.toInt())
            TokenType.REAL_LITERAL -> LiteralNode(token.lexeme.toDouble())
            TokenType.IDENTIFIER -> {
                val name = token.lexeme
                // Function call or variable reference
                if (getNextToken()?.tokenType == TokenType.LPAREN) {
                    advance() // consume '('
                    val args = mutableListOf<ExprNode>()
                    if (getNextToken()?.tokenType != TokenType.RPAREN) {
                        args.add(parseExpression())
                        while (getNextToken()?.tokenType == TokenType.COMMA) {
                            advance()
                            args.add(parseExpression())
                        }
                    }
                    expect(TokenType.RPAREN)
                    CallExprNode(name, args)   // must be an ExprNode subclass
                } else {
                    VarRefNode(name)           // must be an ExprNode subclass
                }
            }
            TokenType.TRUE -> LiteralNode(true)
            TokenType.FALSE -> LiteralNode(false)
            TokenType.LPAREN -> {
                val expr = parseExpression()
                expect(TokenType.RPAREN)
                expr
            }
            TokenType.PLUS, TokenType.MINUS -> {
                val op = token.tokenType.toString()
                val expr = parsePrimary()
                UnaryOpNode(op, expr)         // must be an ExprNode subclass
            }
            else -> error("Unexpected token $token in primary")
        }
    }

    private fun parseNewLineNode(): NewLineNode {
        expect(TokenType.NEW_LINE)
        return NewLineNode()
    }
}
