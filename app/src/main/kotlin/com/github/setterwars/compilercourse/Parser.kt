package com.github.setterwars.compilercourse

/**
 * Basic recursive-descent parser skeleton following Project I grammar.
 *
 * Notes:
 * - Assumes `Token` and `TokenType` are provided by your lexer.
 * - This parser implements the main grammar structure and builds AST nodes
 *   defined in ProgramNode.kt.
 * - TODO: adapt TokenType names if your lexer uses different token names,
 *   add better error reporting, and implement semantic checks as needed.
 */

class Parser(private val tokens: List<Token>) {
    private var pos = 0

    private fun currentToken(): Token? = tokens.getOrNull(pos)
    private fun advance(): Token? = tokens.getOrNull(pos++)
    private fun peek(offset: Int = 0): Token? = tokens.getOrNull(pos + offset)

    private fun expect(kind: TokenType): Token {
        val tok = advance() ?: error("Unexpected EOF: expected $kind")
        if (tok.tokenType != kind) error("Expected $kind but got ${tok.tokenType} at token '$tok'")
        return tok
    }

    fun parseProgram(): ProgramNode {
        val decls = mutableListOf<ASTNode>()
        // skip possible leading newlines/semicolons
        skipSeparators()
        while (currentToken() != null && currentToken()?.tokenType != TokenType.EOF) {
            decls.add(parseTopLevelItem())
            skipSeparators()
        }
        // consume EOF if present
        if (currentToken()?.tokenType == TokenType.EOF) expect(TokenType.EOF)
        return ProgramNode(decls)
    }

    // --- Top-level items: Declarations or Statements ---
    private fun parseTopLevelItem(): ASTNode =
        when (currentToken()?.tokenType) {
            TokenType.VAR -> parseVarDecl()
            TokenType.TYPE -> parseTypeDecl()
            TokenType.ROUTINE -> parseRoutineDecl()
            // statements allowed at top-level (per tests)
            TokenType.IDENTIFIER,
            TokenType.WHILE,
            TokenType.FOR,
            TokenType.IF,
            TokenType.PRINT,
            TokenType.RETURN,
            TokenType.LPAREN,
            TokenType.TRUE,
            TokenType.FALSE,
            TokenType.INT_LITERAL,
            TokenType.REAL_LITERAL,
            TokenType.NOT,
            TokenType.PLUS,
            TokenType.MINUS -> parseStatement()
            else -> error("Unexpected top-level token ${currentToken()}")
        }

    private fun skipSeparators() {
        while (currentToken()?.tokenType == TokenType.NEW_LINE ||
            currentToken()?.tokenType == TokenType.SEMICOLON) {
            advance()
        }
    }

    // --- Variable declaration: var id : Type [ is Expression ] | var id is Expression ---
    private fun parseVarDecl(): VarDeclNode {
        expect(TokenType.VAR)
        val name = expect(TokenType.IDENTIFIER).lexeme
        var type: TypeNode? = null
        var init: ExprNode? = null

        when (currentToken()?.tokenType) {
            TokenType.COLON -> {
                advance()
                type = parseType()
                if (currentToken()?.tokenType == TokenType.IS) {
                    advance()
                    init = parseExpression()
                }
            }
            TokenType.IS -> {
                advance()
                init = parseExpression()
                // type inferred from init; leave type == null to indicate inference
            }
            else -> error("Expected ':' or 'is' after var identifier")
        }

        return VarDeclNode(name, type, init)
    }

    // --- Type declaration: type id is Type ---
    private fun parseTypeDecl(): TypeDeclNode {
        expect(TokenType.TYPE)
        val name = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.IS)
        val base = parseType()
        return TypeDeclNode(name, base)
    }

    // --- Routine declaration: routine id (params) [: Type] [ is Body end | => Expression ] ---
    private fun parseRoutineDecl(): RoutineDeclNode {
        expect(TokenType.ROUTINE)
        val name = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.LPAREN)
        val params = mutableListOf<ParamNode>()
        if (currentToken()?.tokenType != TokenType.RPAREN) {
            params.add(parseParam())
            while (currentToken()?.tokenType == TokenType.COMMA) {
                advance()
                params.add(parseParam())
            }
        }
        expect(TokenType.RPAREN)

        var returnType: TypeNode? = null
        if (currentToken()?.tokenType == TokenType.COLON) {
            advance()
            returnType = parseType()
        }

        val body: BodyNode? = when (currentToken()?.tokenType) {
            TokenType.IS -> {
                advance()
                parseBodyExpectEnd()
            }
            TokenType.ARROW, TokenType.FAT_ARROW -> { // => expression form
                advance()
                val expr = parseExpression()
                BodyNode(listOf(ReturnNode(expr)))
            }
            else -> null // forward declaration
        }

        return RoutineDeclNode(name, params, returnType, body)
    }

    private fun parseParam(): ParamNode {
        val pname = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.COLON)
        val ptype = parseType()
        return ParamNode(pname, ptype)
    }

    // parse body block that ends with END keyword (body contains declarations and statements)
    private fun parseBodyExpectEnd(): BodyNode {
        skipSeparators()
        val items = mutableListOf<ASTNode>()
        while (currentToken() != null && currentToken()?.tokenType != TokenType.END) {
            when (currentToken()?.tokenType) {
                TokenType.VAR -> items.add(parseVarDecl())
                TokenType.TYPE -> items.add(parseTypeDecl())
                else -> items.add(parseStatement())
            }
            skipSeparators()
        }
        expect(TokenType.END)
        return BodyNode(items)
    }

    // --- Statements ---
    private fun parseStatement(): ASTNode {
        return when (currentToken()?.tokenType) {
            TokenType.IDENTIFIER -> {
                // Ambiguity: could be Assignment (ModifiablePrimary := Expr) or RoutineCall
                // We'll parse a modifiable primary and then decide.
                val mp = parseModifiablePrimary()
                // Assignment uses ':=' token in grammar; assume TokenType.ASSIGN or COLON_ASSIGN
                if (currentToken()?.tokenType == TokenType.ASSIGN) {
                    advance()
                    val value = parseExpression()
                    AssignNode(mp, value)
                } else {
                    // it's a routine call statement (call arguments optional)
                    if (mp.selectors.isEmpty() && currentToken()?.tokenType == TokenType.LPAREN) {
                        // treat as call by name
                        val name = mp.base
                        val args = parseCallArgs()
                        CallNode(name, args)
                    } else {
                        // It's a variable reference used as expression statement (allow ExprStmt)
                        ExprStmtNode(ModifiablePrimaryToExpr(mp))
                    }
                }
            }
            TokenType.WHILE -> parseWhile()
            TokenType.FOR -> parseFor()
            TokenType.IF -> parseIf()
            TokenType.PRINT -> parsePrint()
            TokenType.RETURN -> parseReturn()
            else -> {
                // expression statement (e.g., call-expression or literal expression)
                val expr = parseExpression()
                ExprStmtNode(expr)
            }
        }
    }

    // helper: convert a modifiable primary to an ExprNode (VarRef or CallExpr or ModifiablePrimaryNode)
    private fun ModifiablePrimaryToExpr(mp: ModifiablePrimaryNode): ExprNode {
        // If there are no selectors and name is used as plain variable, return VarRefNode
        return if (mp.selectors.isEmpty()) {
            VarRefNode(mp.base)
        } else {
            mp // ModifiablePrimaryNode is itself an ExprNode in our ADT
        }
    }

    private fun parseCallArgs(): List<ExprNode> {
        val args = mutableListOf<ExprNode>()
        expect(TokenType.LPAREN)
        if (currentToken()?.tokenType != TokenType.RPAREN) {
            args.add(parseExpression())
            while (currentToken()?.tokenType == TokenType.COMMA) {
                advance()
                args.add(parseExpression())
            }
        }
        expect(TokenType.RPAREN)
        return args
    }

    private fun parseWhile(): WhileNode {
        expect(TokenType.WHILE)
        val cond = parseExpression()
        expect(TokenType.LOOP)
        val body = parseBodyExpectEnd()
        return WhileNode(cond, body)
    }

    private fun parseFor(): ForNode {
        expect(TokenType.FOR)
        val varName = expect(TokenType.IDENTIFIER).lexeme
        expect(TokenType.IN)
        val startExpr = parseExpression()
        var endExpr: ExprNode? = null
        if (currentToken()?.tokenType == TokenType.DOT) {
            // handle '..' or RANGE token depending on lexer
            advance()
            if (currentToken()?.tokenType == TokenType.DOT) {
                advance()
                endExpr = parseExpression()
            } else {
                error("Expected '.' in range sequence")
            }
        } else if (currentToken()?.tokenType == TokenType.RANGE) {
            advance()
            endExpr = parseExpression()
        } else {
            // single expression case: may denote an array (loop over array)
            endExpr = null
        }
        val reverse = if (currentToken()?.tokenType == TokenType.REVERSE) {
            advance(); true
        } else false
        expect(TokenType.LOOP)
        val body = parseBodyExpectEnd()
        return ForNode(varName, RangeNode(startExpr, endExpr), reverse, body)
    }

    private fun parseIf(): IfNode {
        expect(TokenType.IF)
        val cond = parseExpression()
        expect(TokenType.THEN)
        val thenBody = parseBlockUntil(setOf(TokenType.ELSE, TokenType.END))
        var elseBody: BodyNode? = null
        if (currentToken()?.tokenType == TokenType.ELSE) {
            advance()
            elseBody = parseBlockUntil(setOf(TokenType.END))
        }
        expect(TokenType.END)
        return IfNode(cond, thenBody, elseBody)
    }

    private fun parseBlockUntil(terminators: Set<TokenType>): BodyNode {
        skipSeparators()
        val items = mutableListOf<ASTNode>()
        while (currentToken() != null && currentToken()?.tokenType !in terminators) {
            when (currentToken()?.tokenType) {
                TokenType.VAR -> items.add(parseVarDecl())
                TokenType.TYPE -> items.add(parseTypeDecl())
                else -> items.add(parseStatement())
            }
            skipSeparators()
        }
        return BodyNode(items)
    }

    private fun parsePrint(): PrintNode {
        expect(TokenType.PRINT)
        val args = mutableListOf<ExprNode>()
        args.add(parseExpression())
        while (currentToken()?.tokenType == TokenType.COMMA) {
            advance()
            args.add(parseExpression())
        }
        return PrintNode(args)
    }

    private fun parseReturn(): ReturnNode {
        expect(TokenType.RETURN)
        return if (currentToken()?.tokenType in setOf(
                TokenType.IDENTIFIER,
                TokenType.INT_LITERAL,
                TokenType.REAL_LITERAL,
                TokenType.TRUE,
                TokenType.FALSE,
                TokenType.LPAREN,
                TokenType.PLUS,
                TokenType.MINUS,
                TokenType.NOT
            )) {
            ReturnNode(parseExpression())
        } else {
            ReturnNode()
        }
    }

    // --- Types ---
    private fun parseType(): TypeNode {
        val tok = advance() ?: error("Unexpected EOF while parsing type")
        return when (tok.tokenType) {
            TokenType.INTEGER -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.INTEGER)
            TokenType.REAL -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.REAL)
            TokenType.BOOLEAN -> PrimitiveTypeNode(PrimitiveTypeNode.Kind.BOOLEAN)
            TokenType.ARRAY -> {
                expect(TokenType.LBRACKET)
                // array size is optional (sizeless arrays for parameters)
                val sizeExpr: ExprNode? = if (currentToken()?.tokenType != TokenType.RBRACKET) {
                    parseExpression()
                } else null
                expect(TokenType.RBRACKET)
                val elemType = parseType()
                ArrayTypeNode(sizeExpr, elemType)
            }
            TokenType.RECORD -> {
                // record { VariableDeclaration } end
                val fields = mutableListOf<VarDeclNode>()
                // Allow nested var/type declarations inside record
                while (true) {
                    skipSeparators()
                    val t = currentToken()?.tokenType
                    if (t == TokenType.VAR) fields.add(parseVarDecl())
                    else if (t == TokenType.END) {
                        advance()
                        break
                    } else error("Unexpected token in record type: $t")
                }
                RecordTypeNode(fields)
            }
            TokenType.IDENTIFIER -> NamedTypeNode(tok.lexeme)
            else -> error("Unexpected token in type: $tok")
        }
    }

    // --- Expressions: precedence climbing via separate methods ---
    private fun parseExpression(): ExprNode = parseOrXor()

    private fun parseOrXor(): ExprNode {
        var left = parseAnd()
        while (currentToken()?.tokenType in setOf(TokenType.OR, TokenType.XOR)) {
            val opTok = advance()!!
            val right = parseAnd()
            left = BinaryOpNode(left, opTok.lexeme, right)
        }
        return left
    }

    private fun parseAnd(): ExprNode {
        var left = parseRelation()
        while (currentToken()?.tokenType == TokenType.AND) {
            val opTok = advance()!!
            val right = parseRelation()
            left = BinaryOpNode(left, opTok.lexeme, right)
        }
        return left
    }

    private fun parseRelation(): ExprNode {
        var left = parseSimple()
        while (currentToken()?.tokenType in setOf(
                TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE,
                TokenType.EQ, TokenType.NE // NE corresponds to '/=' in grammar
            )) {
            val op = advance()!!.lexeme
            val right = parseSimple()
            left = BinaryOpNode(left, op, right)
        }
        return left
    }

    private fun parseSimple(): ExprNode {
        var left = parseFactor()
        while (currentToken()?.tokenType in setOf(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            val op = advance()!!.lexeme
            val right = parseFactor()
            left = BinaryOpNode(left, op, right)
        }
        return left
    }

    private fun parseFactor(): ExprNode {
        var left = parseSummand()
        while (currentToken()?.tokenType in setOf(TokenType.PLUS, TokenType.MINUS)) {
            val op = advance()!!.lexeme
            val right = parseSummand()
            left = BinaryOpNode(left, op, right)
        }
        return left
    }

    private fun parseSummand(): ExprNode {
        if (currentToken()?.tokenType == TokenType.LPAREN) {
            advance()
            val inner = parseExpression()
            expect(TokenType.RPAREN)
            return inner
        }
        if (currentToken()?.tokenType == TokenType.NOT) {
            advance()
            val inner = parseSummand()
            return UnaryOpNode("not", inner)
        }
        if (currentToken()?.tokenType == TokenType.PLUS || currentToken()?.tokenType == TokenType.MINUS) {
            val op = advance()!!.lexeme
            val inner = parseSummand()
            return UnaryOpNode(op, inner)
        }
        return parsePrimary()
    }

    private fun parsePrimary(): ExprNode {
        val tok = advance() ?: error("Unexpected EOF in primary")
        return when (tok.tokenType) {
            TokenType.INT_LITERAL -> LiteralNode(tok.lexeme.toInt())
            TokenType.REAL_LITERAL -> LiteralNode(tok.lexeme.toDouble())
            TokenType.TRUE -> LiteralNode(true)
            TokenType.FALSE -> LiteralNode(false)
            TokenType.IDENTIFIER -> {
                // Could be routine call or modifiable primary
                if (currentToken()?.tokenType == TokenType.LPAREN) {
                    // call expression
                    val name = tok.lexeme
                    val args = parseCallArgs()
                    CallExprNode(name, args)
                } else {
                    VarRefNode(tok.lexeme)
                }
            }
            else -> error("Unexpected primary token: $tok")
        }
    }

    // --- ModifiablePrimary: Identifier { . Identifier | [ Expression ] } ---
    private fun parseModifiablePrimary(): ModifiablePrimaryNode {
        val baseName = expect(TokenType.IDENTIFIER).lexeme
        val selectors = mutableListOf<SelectorNode>()
        while (true) {
            when (currentToken()?.tokenType) {
                TokenType.DOT -> {
                    advance()
                    val field = expect(TokenType.IDENTIFIER).lexeme
                    selectors.add(FieldAccessNode(field))
                }
                TokenType.LBRACKET -> {
                    advance()
                    val idx = parseExpression()
                    expect(TokenType.RBRACKET)
                    selectors.add(IndexAccessNode(idx))
                }
                else -> break
            }
        }
        return ModifiablePrimaryNode(baseName, selectors)
    }
}
