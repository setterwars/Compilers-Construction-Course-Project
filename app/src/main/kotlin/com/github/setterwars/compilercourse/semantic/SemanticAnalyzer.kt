package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.parser.nodes.*

class SemanticAnalyzer {

    val info = SemanticInfoStore()
    private val errors = mutableListOf<SemanticError>()

    private val globalScope = SymbolTable(null)
    private var currentScope: SymbolTable = globalScope

    fun analyze(program: Program): AnalysisResult {
        errors.clear()
        currentScope = globalScope
        analyzeProgram(program)
        return AnalysisResult(errors.toList())
    }

    private fun analyzeProgram(p: Program) {
        p.declarations.forEach { analyzeDeclaration(it) }
    }

    private fun analyzeDeclaration(d: Declaration) {
        when (d) {
            is TypeDeclaration -> analyzeTypeDeclaration(d)
            is RoutineDeclaration -> analyzeRoutineDeclaration(d)
            is VariableDeclaration -> analyzeVariableDeclaration(d)
        }
    }

    private fun analyzeTypeDeclaration(td: TypeDeclaration) {
        val name = td.identifier.token.lexeme
        if (currentScope.isDeclaredInCurrentScope(name)) {
            semanticError("Type '$name' already declared", td.identifier.token.span.line); return
        }
        val resolved = analyzeType(td.type, inParamPos = false)
        currentScope.addDeclaredType(name, resolved)
    }

    private fun analyzeRoutineDeclaration(rd: RoutineDeclaration) {
        val header = rd.header
        val name = header.name.token.lexeme
        if (globalScope.isDeclaredInCurrentScope(name)) {
            semanticError("Routine '$name' already declared", header.name.token.span.line); return
        }

        val params = analyzeParameters(header.parameters)
        val ret = header.returnType?.let { analyzeType(it, inParamPos = false) } ?: ResolvedType.Void

        globalScope.addDeclaredRoutine(name, RoutineSymbol(params, ret, rd))

        // Body
        rd.body?.let { body ->
            val saved = currentScope
            currentScope = SymbolTable(globalScope)
            // declare params in routine scope
            header.parameters.parameters.forEachIndexed { idx, pd ->
                currentScope.addDeclaredVariable(pd.name.token.lexeme, params[idx])
            }
            analyzeRoutineBody(body, ret, name, header.name.token.span.line)
            currentScope = saved
        }
    }

    private fun analyzeRoutineBody(
        rb: RoutineBody,
        expectedReturn: ResolvedType,
        routineName: String,
        routineLine0: Int
    ) {
        when (rb) {
            is FullRoutineBody -> analyzeBody(rb.body)
            is SingleExpressionBody -> {
                val (t, _) = analyzeExpression(rb.expression)
                if (expectedReturn != ResolvedType.Void && !assignable(expectedReturn, t)) {
                    semanticError("Wrong return type for routine '$routineName'", routineLine0)
                }
                if (expectedReturn == ResolvedType.Void) {
                    // Using => expr in void routine is allowed but ignored.
                }
            }
        }
    }

    private fun analyzeParameters(ps: Parameters): List<ResolvedType> =
        ps.parameters.map { analyzeParameterDeclaration(it) }

    private fun analyzeParameterDeclaration(pd: ParameterDeclaration): ResolvedType =
        analyzeType(pd.type, inParamPos = true)

    private fun analyzeVariableDeclaration(vd: VariableDeclaration) {
        when (vd) {
            is VariableDeclarationWithType -> {
                val name = vd.identifier.token.lexeme
                if (currentScope.isDeclaredInCurrentScope(name)) {
                    semanticError(
                        "Variable '$name' already declared in this scope",
                        vd.identifier.token.span.line
                    ); return
                }
                val declared = analyzeType(vd.type, inParamPos = false)
                vd.initialValue?.let { iv ->
                    val (it, _) = analyzeExpression(iv)
                    if (!assignable(declared, it)) {
                        semanticError("Cannot initialize '$name' with incompatible type", vd.identifier.token.span.line)
                    }
                }
                currentScope.addDeclaredVariable(name, declared)
            }

            is VariableDeclarationNoType -> {
                val name = vd.identifier.token.lexeme
                if (currentScope.isDeclaredInCurrentScope(name)) {
                    semanticError(
                        "Variable '$name' already declared in this scope",
                        vd.identifier.token.span.line
                    ); return
                }
                val (t, _) = analyzeExpression(vd.initialValue)
                currentScope.addDeclaredVariable(name, t)
            }
        }
    }

    private fun analyzeBody(b: Body) {
        b.bodyElements.forEach {
            when (it) {
                is SimpleDeclaration -> analyzeDeclaration(it)
                is Statement -> analyzeStatement(it)
            }
        }
    }

    private fun analyzeStatement(s: Statement) {
        when (s) {
            is Assignment -> analyzeAssignment(s)
            is RoutineCall -> {
                analyzeRoutineCall(s)
            } // as statement
            is WhileLoop -> analyzeWhileLoop(s)
            is ForLoop -> analyzeForLoop(s)
            is IfStatement -> analyzeIfStatement(s)
            is PrintStatement -> analyzePrintStatement(s)
        }
    }

    private fun analyzeAssignment(a: Assignment) {
        val lt = analyzeModifiablePrimary(a.modifiablePrimary)
        val (rt, _) = analyzeExpression(a.expression)
        if (!assignable(lt, rt)) {
            semanticError("Type mismatch in assignment", a.modifiablePrimary.variable.token.span.line)
        }
    }

    private fun analyzeRoutineCall(call: RoutineCall): ResolvedType {
        // analyze children (arguments)
        call.arguments.forEach { analyzeExpression(it.expression) }

        val name = call.routineName.token.lexeme
        val sym = currentScope.lookupRoutine(name)
        if (sym == null) {
            semanticError("Call to undeclared routine '$name'", call.routineName.token.span.line)
            info.setSemanticInfo(call, RoutineCallSemanticInfo(ResolvedType.Void))
            return ResolvedType.Void
        }
        if (call.arguments.size != sym.parameterTypes.size) {
            semanticError("Wrong number of arguments for '$name'", call.routineName.token.span.line)
        }
        val n = minOf(call.arguments.size, sym.parameterTypes.size)
        for (i in 0 until n) {
            val at = analyzeExpression(call.arguments[i].expression).first
            val pt = sym.parameterTypes[i]
            if (!assignable(pt, at)) {
                semanticError("Argument ${i + 1} type mismatch for '$name'", call.routineName.token.span.line)
            }
        }
        info.setSemanticInfo(call, RoutineCallSemanticInfo(sym.returnType))
        return sym.returnType
    }

    private fun analyzeWhileLoop(w: WhileLoop) {
        analyzeExpression(w.condition)
        val saved = currentScope; currentScope = SymbolTable(saved)
        analyzeBody(w.body)
        currentScope = saved
    }

    private fun analyzeForLoop(f: ForLoop) {
        val saved = currentScope; currentScope = SymbolTable(saved)
        // loop var is integer
        currentScope.addDeclaredVariable(f.loopVariable.token.lexeme, ResolvedType.Integer)

        analyzeRange(f.range)
        analyzeBody(f.body)
        currentScope = saved
    }

    private fun analyzeIfStatement(i: IfStatement) {
        analyzeExpression(i.condition)
        val conditionExpressionNodeInfo = info.get<ExpressionSemanticInfo>(i.condition)
        if (conditionExpressionNodeInfo.type !is ResolvedType.Boolean) {
            semanticError("Condition expression must be boolean type but it is ${conditionExpressionNodeInfo.type} in $i")
        }
        if (conditionExpressionNodeInfo.const != null) {
            info.setSemanticInfo(
                i,
                IfStatementSemanticInfo(compiledCondition = toBool(conditionExpressionNodeInfo.const))
            )
        }

        var saved = currentScope; currentScope = SymbolTable(saved)
        analyzeBody(i.thenBody)
        currentScope = saved
        i.elseBody?.let {
            saved = currentScope; currentScope = SymbolTable(saved)
            analyzeBody(it)
            currentScope = saved
        }
    }

    private fun analyzePrintStatement(p: PrintStatement) {
        analyzeExpression(p.expression)
        p.rest.forEach { analyzeExpression(it) }
    }

    // ---------------- Expressions ----------------

    private fun analyzeExpression(e: Expression): Pair<ResolvedType, PrimitiveTypeValue?> {
        var (t, c) = analyzeRelation(e.relation)
        e.rest?.forEach { (op, r) ->
            val (rt, rc) = analyzeRelation(r)
            val a = c?.let { toBool(it) }
            val b = rc?.let { toBool(it) }
            val out = if (a != null && b != null) {
                val v = when (op) {
                    ExpressionOperator.AND -> a && b
                    ExpressionOperator.OR -> a || b
                    ExpressionOperator.XOR -> (a && !b) || (!a && b)
                }
                BooleanValue(v)
            } else null
            t = ResolvedType.Boolean; c = out
        }
        info.setSemanticInfo(e, ExpressionSemanticInfo(t, c))
        return t to c
    }

    private fun analyzeRelation(r: Relation): Pair<ResolvedType, PrimitiveTypeValue?> {
        val (lt, lc) = analyzeSimple(r.simple)
        if (r.comparison != null) {
            val (op, s) = r.comparison
            val (rt, rc) = analyzeSimple(s)
            val out = if (lc != null && rc != null) {
                val res = when (op) {
                    RelationOperator.LT -> compare(lc, rc) < 0
                    RelationOperator.LE -> compare(lc, rc) <= 0
                    RelationOperator.GT -> compare(lc, rc) > 0
                    RelationOperator.GE -> compare(lc, rc) >= 0
                    RelationOperator.EQ -> equalConst(lc, rc)
                    RelationOperator.NEQ -> !equalConst(lc, rc)
                }
                BooleanValue(res)
            } else null
            info.setSemanticInfo(r, RelationSemanticInfo(ResolvedType.Boolean, out))
            return ResolvedType.Boolean to out
        } else {
            info.setSemanticInfo(r, RelationSemanticInfo(lt, lc))
            return lt to lc
        }
    }

    private fun analyzeSimple(s: Simple): Pair<ResolvedType, PrimitiveTypeValue?> {
        var (t, c) = analyzeFactor(s.factor)
        s.rest?.forEach { (op, f) ->
            val (rt, rc) = analyzeFactor(f)
            val outType = arithmeticWiden(t, rt)
            val outConst = if (c != null && rc != null) {
                when (op) {
                    SimpleOperator.PLUS -> addConst(c, rc)
                    SimpleOperator.MINUS -> subConst(c, rc)
                }
            } else null
            t = outType; c = outConst
        }
        info.setSemanticInfo(s, SimpleSemanticInfo(t, c))
        return t to c
    }

    private fun analyzeFactor(f: Factor): Pair<ResolvedType, PrimitiveTypeValue?> {
        var (t, c) = analyzeSummand(f.summand)
        f.rest?.forEach { (op, s) ->
            val (rt, rc) = analyzeSummand(s)
            val outType = when (op) {
                FactorOperator.PRODUCT -> arithmeticWiden(t, rt)
                FactorOperator.DIVISION -> arithmeticWiden(t, rt)
                FactorOperator.MODULO -> ResolvedType.Integer
            }
            val outConst = if (c != null && rc != null) {
                when (op) {
                    FactorOperator.PRODUCT -> mulConst(c, rc)
                    FactorOperator.DIVISION -> divConst(c, rc)
                    FactorOperator.MODULO -> modConst(c, rc)
                }
            } else null
            t = outType; c = outConst
        }
        info.setSemanticInfo(f, FactorSemanticInfo(t, c))
        return t to c
    }

    private fun analyzeSummand(s: Summand): Pair<ResolvedType, PrimitiveTypeValue?> =
        when (s) {
            is ExpressionInParenthesis -> analyzeExpressionInParenthesis(s)
            is Primary -> analyzePrimary(s)
            else -> ResolvedType.Boolean to null
        }

    private fun analyzeExpressionInParenthesis(ep: ExpressionInParenthesis): Pair<ResolvedType, PrimitiveTypeValue?> =
        analyzeExpression(ep.expression)

    private fun analyzePrimary(p: Primary): Pair<ResolvedType, PrimitiveTypeValue?> =
        when (p) {
            is UnaryInteger -> analyzeUnaryInteger(p)
            is UnaryReal -> analyzeUnaryReal(p)
            is UnaryModifiablePrimary -> analyzeUnaryModifiablePrimary(p)
            is BooleanLiteral -> analyzeBooleanLiteral(p)
            is RoutineCall -> {
                val t = analyzeRoutineCall(p)
                if (t == ResolvedType.Void) {
                    semanticError("Using a void routine in an expression", p.routineName.token.span.line)
                }
                t to null
            }

            else -> ResolvedType.Boolean to null
        }

    private fun analyzeUnaryInteger(ui: UnaryInteger): Pair<ResolvedType, PrimitiveTypeValue?> {
        val rawStr = ui.integerLiteral.token.lexeme
        val raw = rawStr.toLongOrNull()
        if (raw == null) {
            semanticError("Integer literal out of range", ui.integerLiteral.token.span.line)
            return ResolvedType.Integer to null
        }
        info.setSemanticInfo(ui.integerLiteral, IntegerLiteralSemanticInfo(raw))

        return when (ui.unaryOperator) {
            null -> ResolvedType.Integer to IntValue(raw)
            is UnarySign -> {
                val v = if (ui.unaryOperator == UnarySign.MINUS) -raw else raw
                info.setSemanticInfo(ui, UnaryIntegerSemanticInfo(v))
                ResolvedType.Integer to IntValue(v)
            }

            is UnaryNot -> {
                // not <int> → boolean (0=false, else true)
                val asBool = raw != 0L
                info.setSemanticInfo(ui, UnaryIntegerSemanticInfo(if (asBool) 0L else 1L))
                ResolvedType.Boolean to BooleanValue(!asBool)
            }
        }
    }

    private fun analyzeUnaryReal(ur: UnaryReal): Pair<ResolvedType, PrimitiveTypeValue?> {
        val raw = ur.realLiteral.token.lexeme.toDoubleOrNull()
        if (raw == null) {
            semanticError("Invalid real literal", ur.realLiteral.token.span.line)
            return ResolvedType.Real to null
        }
        info.setSemanticInfo(ur.realLiteral, RealLiteralSemanticInfo(raw))
        val v = if (ur.unaryRealOperator == UnarySign.MINUS) -raw else raw
        return ResolvedType.Real to RealValue(v)
    }

    private fun analyzeUnaryModifiablePrimary(ump: UnaryModifiablePrimary): Pair<ResolvedType, PrimitiveTypeValue?> {
        val baseType = analyzeModifiablePrimary(ump.modifiablePrimary)
        return when (ump.unaryOperator) {
            null -> baseType to null
            is UnaryNot -> ResolvedType.Boolean to null // cannot fold without value
            is UnarySign -> {
                // STRICT: cannot use unary sign on non-primitive modifiable primary
                if (!isPrimitive(baseType)) {
                    semanticError(
                        "Unary sign cannot be applied to non-primitive value",
                        ump.modifiablePrimary.variable.token.span.line
                    )
                    ResolvedType.Integer to null
                } else {
                    // arithmetic sign: widen to numeric domain
                    when (baseType) {
                        ResolvedType.Real -> ResolvedType.Real to null
                        ResolvedType.Integer, ResolvedType.Boolean -> ResolvedType.Integer to null
                        else -> ResolvedType.Integer to null
                    }
                }
            }
        }
    }

    private fun analyzeBooleanLiteral(bl: BooleanLiteral): Pair<ResolvedType, PrimitiveTypeValue?> {
        val v = bl == BooleanLiteral.TRUE
        return ResolvedType.Boolean to BooleanValue(v)
    }

    private fun analyzeModifiablePrimary(mp: ModifiablePrimary): ResolvedType {
        var t: ResolvedType = analyzeIdentifier(mp.variable)
        mp.accessors?.forEach { t = analyzeAccessor(t, it) }
        info.setSemanticInfo(mp, ModifiablePrimarySemanticInfo(t))
        return t
    }

    private fun analyzeIdentifier(id: Identifier): ResolvedType {
        val name = id.token.lexeme
        val t = currentScope.lookupVariable(name)
        if (t == null) {
            semanticError("Undeclared variable '$name'", id.token.span.line)
            return ResolvedType.Boolean
        }
        return t
    }

    private fun analyzeAccessor(prev: ResolvedType, acc: Accessor): ResolvedType =
        when (acc) {
            is FieldAccessor -> analyzeFieldAccessor(prev, acc)
            is ArrayAccessor -> analyzeArrayAccessor(prev, acc)
            else -> ResolvedType.Boolean
        }

    private fun analyzeFieldAccessor(prev: ResolvedType, acc: FieldAccessor): ResolvedType {
        if (prev is ResolvedType.Record) {
            val f = acc.identifier.token.lexeme
            val ft = prev.fields[f]
            if (ft == null) {
                semanticError("No such field '$f'", acc.identifier.token.span.line)
                return ResolvedType.Boolean
            }
            return ft
        }
        semanticError("Field access on non-record value", acc.identifier.token.span.line)
        return ResolvedType.Boolean
    }

    private fun analyzeArrayAccessor(prev: ResolvedType, acc: ArrayAccessor): ResolvedType {
        analyzeExpression(acc.expression) // index expr analyzed; strict type check not forced here
        return when (prev) {
            is ResolvedType.SizedArray -> {
                val expressionSemanticInfo = info.get<ExpressionSemanticInfo>(acc.expression)
                if (expressionSemanticInfo.type !is ResolvedType.Integer) {
                    semanticError("Accessor $acc is not integer")
                }
                if ((expressionSemanticInfo.const as IntValue).value !in 1..prev.size) {
                    semanticError("Accessor $acc value is not an integer constant or is not in bounds")
                }
                prev.elementType
            }

            is ResolvedType.UnsizedArray -> prev.elementType
            else -> {
                semanticError(
                    "Indexing non-array value", when (acc) {
                        else -> 0
                    }
                )
                ResolvedType.Boolean
            }
        }
    }

    // ---------------- Types ----------------

    private fun analyzeType(t: Type, inParamPos: Boolean): ResolvedType {
        val resolvedType = when (t) {
            PrimitiveType.INTEGER -> ResolvedType.Integer
            PrimitiveType.REAL -> ResolvedType.Real
            PrimitiveType.BOOLEAN -> ResolvedType.Boolean

            is DeclaredType -> {
                val name = t.identifier.token.lexeme
                currentScope.lookupType(name) ?: run {
                    semanticError("Unknown type '$name'", t.identifier.token.span.line)
                    ResolvedType.Boolean
                }
            }

            is ArrayType -> analyzeArrayType(t, inParamPos)
            is RecordType -> analyzeRecordType(t)
        }
        info.setSemanticInfo(
            t,
            info = TypeSemanticInfo(resolvedType = resolvedType)
        )
        return resolvedType
    }

    private fun analyzeArrayType(at: ArrayType, inParamPos: Boolean): ResolvedType {
        val elem = analyzeType(at.type, inParamPos)
        val const = at.expressionInBrackets?.let { analyzeExpression(it).second }
        val size = const?.let { toLong(it).coerceAtLeast(0).toInt() }
        return if (size != null) ResolvedType.SizedArray(size, elem) else ResolvedType.UnsizedArray(elem)
    }

    private fun analyzeRecordType(rt: RecordType): ResolvedType {
        val fmap = linkedMapOf<String, ResolvedType>()
        rt.declarations.forEach { d ->
            when (d) {
                is VariableDeclarationWithType -> {
                    val fname = d.identifier.token.lexeme
                    if (fmap.containsKey(fname)) {
                        semanticError("Duplicate record field '$fname'", d.identifier.token.span.line)
                    } else {
                        val ftype = analyzeType(d.type, inParamPos = false)
                        d.initialValue?.let { analyzeExpression(it) }
                        fmap[fname] = ftype
                    }
                }

                is VariableDeclarationNoType -> {
                    val fname = d.identifier.token.lexeme
                    if (fmap.containsKey(fname)) {
                        semanticError("Duplicate record field '$fname'", d.identifier.token.span.line)
                    } else {
                        val (ftype, _) = analyzeExpression(d.initialValue)
                        fmap[fname] = ftype
                    }
                }
            }
        }
        return ResolvedType.Record(fmap)
    }

    private fun analyzeRange(r: Range) {
        val (bt, bc) = analyzeExpression(r.begin)
        if (bt != ResolvedType.Integer) {
            semanticError("For-loop begin bound must be integer: $r")
        }
        r.end?.let { e ->
            val (et, _) = analyzeExpression(e)
            if (et != ResolvedType.Integer) {
                semanticError("For-loop end bound must be integer: $r")
            }
        }

    }

    private fun analyzeRoutineHeader(h: RoutineHeader): Pair<List<ResolvedType>, ResolvedType> {
        val ps = analyzeParameters(h.parameters)
        val rt = h.returnType?.let { analyzeType(it, inParamPos = false) } ?: ResolvedType.Void
        return ps to rt
    }

    // Various stuff for primitive types

    private fun assignable(expected: ResolvedType, actual: ResolvedType): Boolean {
        if (isPrimitive(expected) && isPrimitive(actual)) return true // Yes - all primitives are FRIENDS

        when {
            expected is ResolvedType.SizedArray && actual is ResolvedType.SizedArray ->
                return expected.size == actual.size && assignable(expected.elementType, actual.elementType)

            expected is ResolvedType.UnsizedArray && actual is ResolvedType.UnsizedArray ->
                return assignable(expected.elementType, actual.elementType)

            expected is ResolvedType.UnsizedArray && actual is ResolvedType.SizedArray ->
                return assignable(expected.elementType, actual.elementType)
        }

        if (expected is ResolvedType.Record && actual is ResolvedType.Record) {
            if (expected.fields.keys != actual.fields.keys) return false
            return expected.fields.all { (k, et) -> assignable(et, actual.fields.getValue(k)) }
        }

        if (expected == ResolvedType.Void || actual == ResolvedType.Void) return false
        return false
    }

    private fun arithmeticWiden(a: ResolvedType, b: ResolvedType): ResolvedType {
        if (a == ResolvedType.Real || b == ResolvedType.Real) return ResolvedType.Real
        if (a == ResolvedType.Integer || b == ResolvedType.Integer) return ResolvedType.Integer
        return ResolvedType.Integer // boolean + boolean -> integer domain
    }

    private fun isPrimitive(t: ResolvedType) = t is ResolvedType.ResolvedPrimitiveType

    // Helpers for constants

    private fun toBool(v: PrimitiveTypeValue): Boolean = when (v) {
        is BooleanValue -> v.value
        is IntValue -> v.value != 0L
        is RealValue -> v.value != 0.0
    }

    private fun toLong(v: PrimitiveTypeValue): Long = when (v) {
        is BooleanValue -> if (v.value) 1L else 0L
        is IntValue -> v.value
        is RealValue -> v.value.toLong()
    }

    private fun toDouble(v: PrimitiveTypeValue): Double = when (v) {
        is BooleanValue -> if (v.value) 1.0 else 0.0
        is IntValue -> v.value.toDouble()
        is RealValue -> v.value
    }

    private fun equalConst(a: PrimitiveTypeValue, b: PrimitiveTypeValue): Boolean {
        return when {
            a is RealValue || b is RealValue -> toDouble(a) == toDouble(b)
            a is IntValue || b is IntValue -> toLong(a) == toLong(b)
            else -> (a as BooleanValue).value == (b as BooleanValue).value
        }
    }

    /** a < b <=> a - b V 0
     * negative <=> a - b < 0 <=> a < b
     * positive <=> a - b > 0 <=> a > b
     * zero <=> a - b = 0 <=> a == */
    private fun compare(a: PrimitiveTypeValue, b: PrimitiveTypeValue): Int {
        return when {
            a is RealValue || b is RealValue -> toDouble(a).compareTo(toDouble(b))
            else -> toLong(a).compareTo(toLong(b))
        }
    }

    private fun addConst(a: PrimitiveTypeValue, b: PrimitiveTypeValue): PrimitiveTypeValue =
        if (a is RealValue || b is RealValue) RealValue(toDouble(a) + toDouble(b))
        else IntValue(toLong(a) + toLong(b))

    private fun subConst(a: PrimitiveTypeValue, b: PrimitiveTypeValue): PrimitiveTypeValue =
        if (a is RealValue || b is RealValue) RealValue(toDouble(a) - toDouble(b))
        else IntValue(toLong(a) - toLong(b))

    private fun mulConst(a: PrimitiveTypeValue, b: PrimitiveTypeValue): PrimitiveTypeValue =
        if (a is RealValue || b is RealValue) RealValue(toDouble(a) * toDouble(b))
        else IntValue(toLong(a) * toLong(b))

    private fun divConst(a: PrimitiveTypeValue, b: PrimitiveTypeValue): PrimitiveTypeValue {
        val denomD = toDouble(b)
        if (denomD == 0.0) return RealValue(Double.NaN)
        return if (a is RealValue || b is RealValue) {
            RealValue(toDouble(a) / denomD)
        } else {
            val denom = toLong(b)
            if (denom == 0L) RealValue(Double.NaN) else IntValue(toLong(a) / denom)
        }
    }

    private fun modConst(a: PrimitiveTypeValue, b: PrimitiveTypeValue): PrimitiveTypeValue {
        val denom = toLong(b)
        if (denom == 0L) return IntValue(0)
        return IntValue(toLong(a) % denom)
    }

    // Diagnostics Stuff

    private fun semanticError(message: String, lineZeroBased: Int? = null) {
        errors += SemanticError(message, lineZeroBased?.plus(1))
    }
}

data class AnalysisResult(val errors: List<SemanticError>)

data class SemanticError(
    val message: String,
    val line: Int?
) {
    override fun toString(): String = "Semantic error ${line?.let { "at line $line" } ?: ""} : $message"
}
