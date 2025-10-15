package com.github.setterwars.compilercourse.parser

import com.github.setterwars.compilercourse.lexer.Token
import com.github.setterwars.compilercourse.lexer.TokenType
import kotlin.math.max

class Parser(private val tokens: List<Token>) {

    // Public API
    fun parseTokens(): ProgramNode {
        try {
            val res = parseProgram(0) ?: throw syntaxError("program", 0)
            var i = skipLayout(res.next)
            match(i, TokenType.EOF)?.let { return res.node }
            throw syntaxError("EOF", i)
        } catch (ex: Exception) {
            if (farthestIndex in tokens.indices) {
                println("Hint: The farthest explored token is ${tokens[farthestIndex]}")
            }
            throw ex
        }

    }

    // Some utility stuff
    private data class Result<out T : Symbol>(val node: T, val next: Int)

    private fun at(i: Int): Token? = if (i in tokens.indices) tokens[i] else null

    private fun typeAt(i: Int): TokenType? = at(i)?.tokenType

    private fun wrap(t: Token): Terminal = when (t.tokenType) {
        TokenType.IDENTIFIER -> TIdentifier(t)
        TokenType.INT_LITERAL -> TIntLiteral(t)
        TokenType.REAL_LITERAL -> TRealLiteral(t)
        TokenType.FALSE -> TFalse(t)
        TokenType.TRUE -> TTrue(t)

        TokenType.COLON -> TColon(t)
        TokenType.LBRACKET -> TLBracket(t)
        TokenType.RBRACKET -> TRBracket(t)
        TokenType.ASSIGN -> TAssign(t)
        TokenType.COMMA -> TComma(t)
        TokenType.LPAREN -> TLParen(t)
        TokenType.RPAREN -> TRParen(t)
        TokenType.RANGE -> TRangeDots(t)
        TokenType.ARROW -> TArrow(t)
        TokenType.SEMICOLON -> TSemicolon(t)

        TokenType.AND -> TAnd(t)
        TokenType.OR -> TOr(t)
        TokenType.XOR -> TXor(t)

        TokenType.LT -> TLt(t)
        TokenType.LE -> TLe(t)
        TokenType.GT -> TGt(t)
        TokenType.GE -> TGe(t)
        TokenType.EQ -> TEq(t)
        TokenType.NE -> TNe(t)

        TokenType.PLUS -> TPlus(t)
        TokenType.MINUS -> TMinus(t)
        TokenType.STAR -> TStar(t)
        TokenType.SLASH -> TSlash(t)
        TokenType.PERCENT -> TPercent(t)
        TokenType.DOT -> TDot(t)

        TokenType.VAR -> TVar(t)
        TokenType.IS -> TIs(t)
        TokenType.TYPE -> TTypeKw(t)
        TokenType.INTEGER -> TIntegerKw(t)
        TokenType.REAL -> TRealKw(t)
        TokenType.BOOLEAN -> TBooleanKw(t)
        TokenType.RECORD -> TRecord(t)
        TokenType.END -> TEnd(t)
        TokenType.ARRAY -> TArray(t)
        TokenType.WHILE -> TWhile(t)
        TokenType.LOOP -> TLoop(t)
        TokenType.FOR -> TFor(t)
        TokenType.IN -> TIn(t)
        TokenType.REVERSE -> TReverse(t)
        TokenType.IF -> TIf(t)
        TokenType.THEN -> TThen(t)
        TokenType.ELSE -> TElse(t)
        TokenType.PRINT -> TPrint(t)
        TokenType.ROUTINE -> TRoutine(t)
        TokenType.NOT -> TNot(t)

        TokenType.NEW_LINE -> TNewLine(t)
        TokenType.EOF -> TEof(t)
    }

    private fun match(i0: Int, expected: TokenType): Result<Terminal>? {
        val i = skipLayout(i0)
        val t = at(i) ?: return null
        return if (t.tokenType == expected) Result(wrap(t), i + 1) else null
    }

    private fun matchAny(i0: Int, vararg types: TokenType): Result<Terminal>? {
        val i = skipLayout(i0)
        val t = at(i) ?: return null
        return if (types.any { it == t.tokenType }) Result(wrap(t), i + 1) else null
    }

    private var farthestIndex = 0

    private fun skipLayout(i0: Int): Int {
        var i = i0
        while (true) {
            val tt = typeAt(i) ?: break
            farthestIndex = max(farthestIndex, i)
            if (tt == TokenType.NEW_LINE) i++ else break
        }
        return i
    }

    private fun syntaxError(expected: String, pos: Int): IllegalArgumentException {
        val t = at(pos)
        val where = if (t != null)
            "line ${t.span.line}, col ${t.span.firstColumn} (found ${t.tokenType} '${t.lexeme}')"
        else "at end of input"
        return IllegalArgumentException("Expected $expected, $where")
    }

    // Grammar
    // Program : { SimpleDeclaration | RoutineDeclaration }
    private fun parseProgram(i0: Int): Result<ProgramNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        while (true) {
            val before = i
            val sd = parseSimpleDeclaration(i)
            if (sd != null) {
                kids += sd.node; i = skipLayout(sd.next); continue
            }
            val rd = parseRoutineDeclaration(i)
            if (rd != null) {
                kids += rd.node; i = skipLayout(rd.next); continue
            }
            if (i == before) break
        }
        return Result(ProgramNode(kids), i)
    }

    // SimpleDeclaration : VariableDeclaration | TypeDeclaration
    private fun parseSimpleDeclaration(i0: Int): Result<SimpleDeclarationNode>? {
        val i = skipLayout(i0)
        parseVariableDeclaration(i)?.let { return Result(SimpleDeclarationNode(listOf(it.node)), it.next) }
        parseTypeDeclaration(i)?.let { return Result(SimpleDeclarationNode(listOf(it.node)), it.next) }
        return null
    }

    // VariableDeclaration :
    //   var Identifier : Type [ is Expression ]
    // | var Identifier is Expression
    private fun parseVariableDeclaration(i0: Int): Result<VariableDeclarationNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()

        val kwVar = match(i, TokenType.VAR) ?: return null
        kids += kwVar.node; i = skipLayout(kwVar.next)

        val id = match(i, TokenType.IDENTIFIER) ?: return null
        kids += id.node; i = skipLayout(id.next)

        // Lookahead for ":" or "is"
        when (typeAt(skipLayout(i))) {
            TokenType.COLON -> {
                val colon = match(i, TokenType.COLON)!!; kids += colon.node; i = skipLayout(colon.next)
                val type = parseType(i) ?: return null
                kids += type.node; i = skipLayout(type.next)
                // optional "is Expression"
                match(i, TokenType.IS)?.let { isTok ->
                    kids += isTok.node; i = skipLayout(isTok.next)
                    val expr = parseExpression(i) ?: return null
                    kids += expr.node; i = skipLayout(expr.next)
                }
            }
            TokenType.IS -> {
                val isTok = match(i, TokenType.IS)!!; kids += isTok.node; i = skipLayout(isTok.next)
                val expr = parseExpression(i) ?: return null
                kids += expr.node; i = skipLayout(expr.next)
            }
            else -> return null
        }

        return Result(VariableDeclarationNode(kids), i)
    }

    // TypeDeclaration : type Identifier is Type
    private fun parseTypeDeclaration(i0: Int): Result<TypeDeclarationNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val typeKw = match(i, TokenType.TYPE) ?: return null
        kids += typeKw.node; i = skipLayout(typeKw.next)

        val name = match(i, TokenType.IDENTIFIER) ?: return null
        kids += name.node; i = skipLayout(name.next)

        val isTok = match(i, TokenType.IS) ?: return null
        kids += isTok.node; i = skipLayout(isTok.next)

        val ty = parseType(i) ?: return null
        kids += ty.node; i = skipLayout(ty.next)
        return Result(TypeDeclarationNode(kids), i)
    }

    // Type : PrimitiveType | UserType | Identifier
    private fun parseType(i0: Int): Result<TypeNode>? {
        val i = skipLayout(i0)
        parsePrimitiveType(i)?.let { return Result(TypeNode(listOf(it.node)), it.next) }
        parseUserType(i)?.let { return Result(TypeNode(listOf(it.node)), it.next) }
        match(i, TokenType.IDENTIFIER)?.let { id -> return Result(TypeNode(listOf(id.node)), id.next) }
        return null
    }

    // PrimitiveType : integer | real | boolean
    private fun parsePrimitiveType(i0: Int): Result<PrimitiveTypeNode>? {
        val i = skipLayout(i0)
        val m = matchAny(i, TokenType.INTEGER, TokenType.REAL, TokenType.BOOLEAN) ?: return null
        return Result(PrimitiveTypeNode(listOf(m.node)), m.next)
    }

    // UserType : ArrayType | RecordType
    private fun parseUserType(i0: Int): Result<UserTypeNode>? {
        val i = skipLayout(i0)
        parseArrayType(i)?.let { return Result(UserTypeNode(listOf(it.node)), it.next) }
        parseRecordType(i)?.let { return Result(UserTypeNode(listOf(it.node)), it.next) }
        return null
    }

    // RecordType : record { VariableDeclaration } end
    private fun parseRecordType(i0: Int): Result<RecordTypeNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val rec = match(i, TokenType.RECORD) ?: return null
        kids += rec.node; i = skipLayout(rec.next)
        while (true) {
            val before = i
            val vd = parseVariableDeclaration(i) ?: break
            kids += vd.node; i = skipLayout(vd.next)
            if (i == before) break
        }
        val end = match(i, TokenType.END) ?: return null
        kids += end.node; i = skipLayout(end.next)
        return Result(RecordTypeNode(kids), i)
    }

    // ArrayType : array [ [ Expression ] ] Type
    // (i.e., 'array' '[' (optional Expression) ']' Type)
    private fun parseArrayType(i0: Int): Result<ArrayTypeNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val arr = match(i, TokenType.ARRAY) ?: return null
        kids += arr.node; i = skipLayout(arr.next)

        val lb = match(i, TokenType.LBRACKET) ?: return null
        kids += lb.node; i = skipLayout(lb.next)
        parseExpression(i)?.let { expr ->
            kids += expr.node; i = skipLayout(expr.next)
        }
        val rb = match(i, TokenType.RBRACKET) ?: return null
        kids += rb.node; i = skipLayout(rb.next)

        val ty = parseType(i) ?: return null
        kids += ty.node; i = skipLayout(ty.next)

        return Result(ArrayTypeNode(kids), i)
    }

    // Statement : Assignment | RoutineCall | WhileLoop | ForLoop | IfStatement | PrintStatement
    private fun parseStatement(i0: Int): Result<StatementNode>? {
        val i = skipLayout(i0)

        // try keyword-led statements first for fast-path + unambiguous parse
        parseWhileLoop(i)?.let { return Result(StatementNode(listOf(it.node)), it.next) }
        parseForLoop(i)?.let { return Result(StatementNode(listOf(it.node)), it.next) }
        parseIfStatement(i)?.let { return Result(StatementNode(listOf(it.node)), it.next) }
        parsePrintStatement(i)?.let { return Result(StatementNode(listOf(it.node)), it.next) }

        // identifier-led: try Assignment first, then RoutineCall
        parseAssignment(i)?.let { return Result(StatementNode(listOf(it.node)), it.next) }
        parseRoutineCall(i)?.let { return Result(StatementNode(listOf(it.node)), it.next) }

        return null
    }

    // Assignment : ModifiablePrimary := Expression
    private fun parseAssignment(i0: Int): Result<AssignmentNode>? {
        var i = skipLayout(i0)
        val mp = parseModifiablePrimary(i) ?: return null
        i = skipLayout(mp.next)
        val asg = match(i, TokenType.ASSIGN) ?: return null
        i = skipLayout(asg.next)
        val expr = parseExpression(i) ?: return null
        return Result(AssignmentNode(listOf(mp.node, asg.node, expr.node)), skipLayout(expr.next))
    }

    // RoutineCall : Identifier [ ( Expression { , Expression } ) ]
    private fun parseRoutineCall(i0: Int): Result<RoutineCallNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val id = match(i, TokenType.IDENTIFIER) ?: return null
        kids += id.node; i = skipLayout(id.next)

        // Optional argument list
        if (typeAt(i) == TokenType.LPAREN) {
            val lp = match(i, TokenType.LPAREN)!!; kids += lp.node; i = skipLayout(lp.next)
            // Require at least one Expression if '(' present (per your EBNF)
            val first = parseExpression(i) ?: return null
            kids += first.node; i = skipLayout(first.next)
            while (true) {
                val comma = match(i, TokenType.COMMA) ?: break
                kids += comma.node; i = skipLayout(comma.next)
                val arg = parseExpression(i) ?: return null
                kids += arg.node; i = skipLayout(arg.next)
            }
            val rp = match(i, TokenType.RPAREN) ?: return null
            kids += rp.node; i = skipLayout(rp.next)
        }

        return Result(RoutineCallNode(kids), i)
    }

    // WhileLoop : while Expression loop Body end
    private fun parseWhileLoop(i0: Int): Result<WhileLoopNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val w = match(i, TokenType.WHILE) ?: return null
        kids += w.node; i = skipLayout(w.next)
        val cond = parseExpression(i) ?: return null
        kids += cond.node; i = skipLayout(cond.next)
        val lp = match(i, TokenType.LOOP) ?: return null
        kids += lp.node; i = skipLayout(lp.next)
        val body = parseBody(i) ?: return null
        kids += body.node; i = skipLayout(body.next)
        val end = match(i, TokenType.END) ?: return null
        kids += end.node; i = skipLayout(end.next)
        return Result(WhileLoopNode(kids), i)
    }

    // ForLoop : for Identifier in Range [ reverse ] loop Body end
    private fun parseForLoop(i0: Int): Result<ForLoopNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val f = match(i, TokenType.FOR) ?: return null
        kids += f.node; i = skipLayout(f.next)
        val id = match(i, TokenType.IDENTIFIER) ?: return null
        kids += id.node; i = skipLayout(id.next)
        val inn = match(i, TokenType.IN) ?: return null
        kids += inn.node; i = skipLayout(inn.next)
        val rng = parseRange(i) ?: return null
        kids += rng.node; i = skipLayout(rng.next)
        match(i, TokenType.REVERSE)?.let { rev ->
            kids += rev.node; i = skipLayout(rev.next)
        }
        val lp = match(i, TokenType.LOOP) ?: return null
        kids += lp.node; i = skipLayout(lp.next)
        val body = parseBody(i) ?: return null
        kids += body.node; i = skipLayout(body.next)
        val end = match(i, TokenType.END) ?: return null
        kids += end.node; i = skipLayout(end.next)
        return Result(ForLoopNode(kids), i)
    }

    // Range : Expression [ .. Expression ]
    private fun parseRange(i0: Int): Result<RangeNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val start = parseExpression(i) ?: return null
        kids += start.node; i = skipLayout(start.next)
        match(i, TokenType.RANGE)?.let { dots ->
            kids += dots.node; i = skipLayout(dots.next)
            val end = parseExpression(i) ?: return null
            kids += end.node; i = skipLayout(end.next)
        }
        return Result(RangeNode(kids), i)
    }

    // IfStatement : if Expression then Body [ else Body ] end
    private fun parseIfStatement(i0: Int): Result<IfStatementNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val iff = match(i, TokenType.IF) ?: return null
        kids += iff.node; i = skipLayout(iff.next)
        val cond = parseExpression(i) ?: return null
        kids += cond.node; i = skipLayout(cond.next)
        val then = match(i, TokenType.THEN) ?: return null
        kids += then.node; i = skipLayout(then.next)
        val thenBody = parseBody(i) ?: return null
        kids += thenBody.node; i = skipLayout(thenBody.next)
        match(i, TokenType.ELSE)?.let { elsTok ->
            kids += elsTok.node; i = skipLayout(elsTok.next)
            val elseBody = parseBody(i) ?: return null
            kids += elseBody.node; i = skipLayout(elseBody.next)
        }
        val end = match(i, TokenType.END) ?: return null
        kids += end.node; i = skipLayout(end.next)
        return Result(IfStatementNode(kids), i)
    }

    // PrintStatement : print Expression { , Expression }
    private fun parsePrintStatement(i0: Int): Result<PrintStatementNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val pr = match(i, TokenType.PRINT) ?: return null
        kids += pr.node; i = skipLayout(pr.next)
        val first = parseExpression(i) ?: return null
        kids += first.node; i = skipLayout(first.next)
        while (true) {
            val comma = match(i, TokenType.COMMA) ?: break
            kids += comma.node; i = skipLayout(comma.next)
            val expr = parseExpression(i) ?: return null
            kids += expr.node; i = skipLayout(expr.next)
        }
        return Result(PrintStatementNode(kids), i)
    }

    // RoutineDeclaration : RoutineHeader [ RoutineBody ]
    private fun parseRoutineDeclaration(i0: Int): Result<RoutineDeclarationNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val hdr = parseRoutineHeader(i) ?: return null
        kids += hdr.node; i = skipLayout(hdr.next)
        parseRoutineBody(i)?.let { body ->
            kids += body.node; i = skipLayout(body.next)
        }
        return Result(RoutineDeclarationNode(kids), i)
    }

    // RoutineHeader : routine Identifier ( Parameters ) [ : Type ]
    private fun parseRoutineHeader(i0: Int): Result<RoutineHeaderNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val r = match(i, TokenType.ROUTINE) ?: return null
        kids += r.node; i = skipLayout(r.next)
        val name = match(i, TokenType.IDENTIFIER) ?: return null
        kids += name.node; i = skipLayout(name.next)
        val lp = match(i, TokenType.LPAREN) ?: return null
        kids += lp.node; i = skipLayout(lp.next)

        val params = parseParameters(i) ?: return null // per EBNF: must have ≥1 parameter
        kids += params.node; i = skipLayout(params.next)

        val rp = match(i, TokenType.RPAREN) ?: return null
        kids += rp.node; i = skipLayout(rp.next)

        match(i, TokenType.COLON)?.let { colon ->
            kids += colon.node; i = skipLayout(colon.next)
            val ty = parseType(i) ?: return null
            kids += ty.node; i = skipLayout(ty.next)
        }

        return Result(RoutineHeaderNode(kids), i)
    }

    // RoutineBody : is Body end | => Expression
    private fun parseRoutineBody(i0: Int): Result<RoutineBodyNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()

        // Try "is Body end" first
        val isTok = match(i, TokenType.IS)
        if (isTok != null) {
            kids += isTok.node; i = skipLayout(isTok.next)
            val body = parseBody(i) ?: return null
            kids += body.node; i = skipLayout(body.next)
            val end = match(i, TokenType.END) ?: return null
            kids += end.node; i = skipLayout(end.next)
            return Result(RoutineBodyNode(kids), i)
        }

        // Try "=> Expression"
        matchAny(i, TokenType.ARROW)?.let { arrow ->
            kids += arrow.node; i = skipLayout(arrow.next)
            val expr = parseExpression(i) ?: return null
            kids += expr.node; i = skipLayout(expr.next)
            return Result(RoutineBodyNode(kids), i)
        }

        return null
    }

    // Parameters : ParameterDeclaration { , ParameterDeclaration }
    private fun parseParameters(i0: Int): Result<ParametersNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val p1 = parseParameterDeclaration(i) ?: return Result(ParametersNode(emptyList()), i)
        kids += p1.node; i = skipLayout(p1.next)
        while (true) {
            val comma = match(i, TokenType.COMMA) ?: break
            kids += comma.node; i = skipLayout(comma.next)
            val p = parseParameterDeclaration(i) ?: return null
            kids += p.node; i = skipLayout(p.next)
        }
        return Result(ParametersNode(kids), i)
    }

    // ParameterDeclaration : Identifier : Type
    private fun parseParameterDeclaration(i0: Int): Result<ParameterDeclarationNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val id = match(i, TokenType.IDENTIFIER) ?: return null
        kids += id.node; i = skipLayout(id.next)
        val colon = match(i, TokenType.COLON) ?: return null
        kids += colon.node; i = skipLayout(colon.next)
        val ty = parseType(i) ?: return null
        kids += ty.node; i = skipLayout(ty.next)
        return Result(ParameterDeclarationNode(kids), i)
    }

    // Body : { SimpleDeclaration | Statement }
    private fun parseBody(i0: Int): Result<BodyNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        while (true) {
            val before = i
            val sd = parseSimpleDeclaration(i)
            if (sd != null) {
                kids += sd.node; i = skipLayout(sd.next); continue
            }
            val st = parseStatement(i)
            if (st != null) {
                kids += st.node; i = skipLayout(st.next); continue
            }
            if (i == before) break
        }
        return Result(BodyNode(kids), i)
    }

    // Expression : Relation { ( and | or | xor ) Relation }
    private fun parseExpression(i0: Int): Result<ExpressionNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val first = parseRelation(i) ?: return null
        kids += first.node; i = skipLayout(first.next)
        while (true) {
            val op = matchAny(i, TokenType.AND, TokenType.OR, TokenType.XOR) ?: break
            kids += op.node; i = skipLayout(op.next)
            val rhs = parseRelation(i) ?: return null
            kids += rhs.node; i = skipLayout(rhs.next)
        }
        return Result(ExpressionNode(kids), i)
    }

    // Relation : Simple [ ( < | <= | > | >= | = | /= ) Simple ]
    private fun parseRelation(i0: Int): Result<RelationNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val left = parseSimple(i) ?: return null
        kids += left.node; i = skipLayout(left.next)
        when (typeAt(i)) {
            TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE, TokenType.EQ, TokenType.NE -> {
                val op = matchAny(i, TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE, TokenType.EQ, TokenType.NE)!!
                kids += op.node; i = skipLayout(op.next)
                val right = parseSimple(i) ?: return null
                kids += right.node; i = skipLayout(right.next)
            }
            else -> {}
        }
        return Result(RelationNode(kids), i)
    }

    // Simple : Factor { ( + | - ) Factor }    // (fixed precedence)
    private fun parseSimple(i0: Int): Result<SimpleNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val first = parseFactor(i) ?: return null
        kids += first.node; i = skipLayout(first.next)
        while (true) {
            val op = matchAny(i, TokenType.PLUS, TokenType.MINUS) ?: break
            kids += op.node; i = skipLayout(op.next)
            val rhs = parseFactor(i) ?: return null
            kids += rhs.node; i = skipLayout(rhs.next)
        }
        return Result(SimpleNode(kids), i)
    }

    // Factor : Summand { ( * | / | % ) Summand }
    private fun parseFactor(i0: Int): Result<FactorNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val first = parseSummand(i) ?: return null
        kids += first.node; i = skipLayout(first.next)
        while (true) {
            val op = matchAny(i, TokenType.STAR, TokenType.SLASH, TokenType.PERCENT) ?: break
            kids += op.node; i = skipLayout(op.next)
            val rhs = parseSummand(i) ?: return null
            kids += rhs.node; i = skipLayout(rhs.next)
        }
        return Result(FactorNode(kids), i)
    }

    // Summand : Primary | ( Expression )
    private fun parseSummand(i0: Int): Result<SummandNode>? {
        var i = skipLayout(i0)
        if (typeAt(i) == TokenType.LPAREN) {
            val kids = mutableListOf<Symbol>()
            val lp = match(i, TokenType.LPAREN)!!; kids += lp.node; i = skipLayout(lp.next)
            val expr = parseExpression(i) ?: return null
            kids += expr.node; i = skipLayout(expr.next)
            val rp = match(i, TokenType.RPAREN) ?: return null
            kids += rp.node; i = skipLayout(rp.next)
            return Result(SummandNode(kids), i)
        }
        val prim = parsePrimary(i) ?: return null
        return Result(SummandNode(listOf(prim.node)), prim.next)
    }

    // Primary :
    //   [ Sign | not ] IntegerLiteral
    // | [ Sign ] RealLiteral
    // | true | false
    // | ModifiablePrimary
    // | RoutineCall
    //
    // Disambiguation:
    // - If IDENTIFIER followed by LPAREN -> RoutineCall
    // - Else -> ModifiablePrimary
    private fun parsePrimary(i0: Int): Result<PrimaryNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val tt = typeAt(i) ?: return null

        fun signOrNot(): Result<SignNode>? {
            when (typeAt(i)) {
                TokenType.PLUS, TokenType.MINUS -> {
                    val s = matchAny(i, TokenType.PLUS, TokenType.MINUS)!!
                    val sn = SignNode(listOf(s.node))
                    return Result(sn, skipLayout(s.next))
                }
                TokenType.NOT -> {
                    val n = match(i, TokenType.NOT)!!
                    val sn = SignNode(listOf(n.node))
                    return Result(sn, skipLayout(n.next))
                }
                else -> return null
            }
        }

        // [Sign|not] IntegerLiteral
        run {
            val save = i
            val prefix = signOrNot()
            val i2 = prefix?.next ?: i
            if (typeAt(i2) == TokenType.INT_LITERAL) {
                prefix?.let { kids += it.node }
                val lit = match(i2, TokenType.INT_LITERAL)!!
                kids += lit.node
                return Result(PrimaryNode(kids), skipLayout(lit.next))
            }
            i = save
        }

        // [Sign] RealLiteral
        run {
            val save = i
            val prefix = if (typeAt(i) == TokenType.PLUS || typeAt(i) == TokenType.MINUS) {
                val s = matchAny(i, TokenType.PLUS, TokenType.MINUS)!!
                SignNode(listOf(s.node)).let { Result(it, skipLayout(s.next)) }
            } else null
            val i2 = prefix?.next ?: i
            if (typeAt(i2) == TokenType.REAL_LITERAL) {
                prefix?.let { kids += it.node }
                val lit = match(i2, TokenType.REAL_LITERAL)!!
                kids += lit.node
                return Result(PrimaryNode(kids), skipLayout(lit.next))
            }
            i = save
        }

        // true | false
        if (tt == TokenType.TRUE || tt == TokenType.FALSE) {
            val b = matchAny(i, TokenType.TRUE, TokenType.FALSE)!!
            kids += b.node
            return Result(PrimaryNode(kids), skipLayout(b.next))
        }

        // Identifier-led: RoutineCall if '(' after; otherwise ModifiablePrimary
        if (tt == TokenType.IDENTIFIER) {
            val afterId = skipLayout(i + 1)
            if (typeAt(afterId) == TokenType.LPAREN) {
                val call = parseRoutineCall(i) ?: return null
                kids += call.node
                return Result(PrimaryNode(kids), call.next)
            } else {
                val mp = parseModifiablePrimary(i) ?: return null
                kids += mp.node
                return Result(PrimaryNode(kids), mp.next)
            }
        }

        return null
    }

    // Sign : + | -         (modeled as NonTerminal wrapping the terminal)
    private fun parseSign(i0: Int): Result<SignNode>? {
        val i = skipLayout(i0)
        val s = matchAny(i, TokenType.PLUS, TokenType.MINUS) ?: return null
        return Result(SignNode(listOf(s.node)), s.next)
    }

    // ModifiablePrimary : Identifier { . Identifier | [ Expression ] }
    private fun parseModifiablePrimary(i0: Int): Result<ModifiablePrimaryNode>? {
        var i = skipLayout(i0)
        val kids = mutableListOf<Symbol>()
        val id = match(i, TokenType.IDENTIFIER) ?: return null
        kids += id.node; i = skipLayout(id.next)
        loop@ while (true) {
            when (typeAt(i)) {
                TokenType.DOT -> {
                    val dot = match(i, TokenType.DOT)!!; kids += dot.node; i = skipLayout(dot.next)
                    val id2 = match(i, TokenType.IDENTIFIER) ?: return null
                    kids += id2.node; i = skipLayout(id2.next)
                }
                TokenType.LBRACKET -> {
                    val lb = match(i, TokenType.LBRACKET)!!; kids += lb.node; i = skipLayout(lb.next)
                    val expr = parseExpression(i) ?: return null
                    kids += expr.node; i = skipLayout(expr.next)
                    val rb = match(i, TokenType.RBRACKET) ?: return null
                    kids += rb.node; i = skipLayout(rb.next)
                }
                else -> break@loop
            }
        }
        return Result(ModifiablePrimaryNode(kids), i)
    }
}

/* =======================
 * (Optional) Debug dump
 * ======================= */
fun Symbol.dump(indent: String = ""): String = when (this) {
    is Terminal -> "$indent${this::class.simpleName}(${token.tokenType}, '${token.lexeme}')"
    is NonTerminal -> {
        val inner = children.joinToString("\n") { it.dump(indent + "  ") }
        "$indent${this::class.simpleName}[\n$inner\n$indent]"
    }
}
