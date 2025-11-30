package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.codegen.utils.toInt
import com.github.setterwars.compilercourse.parser.nodes.*
import com.github.setterwars.compilercourse.semantic.semanticData.DeclaredTypeSemanticData
import com.github.setterwars.compilercourse.semantic.semanticData.ExpressionSemanticData
import com.github.setterwars.compilercourse.semantic.semanticData.ModifiablePrimarySemanticData
import com.github.setterwars.compilercourse.semantic.semanticData.RoutineDeclarationSemanticData
import com.github.setterwars.compilercourse.semantic.semanticData.UnaryIntegerSemanticData
import com.github.setterwars.compilercourse.semantic.semanticData.UnaryRealSemanticData

class SemanticException(
    message: String = "",
    val line: Int = -1,
) : Exception(message)

class SemanticAnalyzer {
    private val globalScope = SymbolTable(null)
    private var currentScope: SymbolTable = globalScope
    var currentRoutine: String? = null

    fun analyze(program: Program) {
        currentScope = globalScope
        analyzeProgram(program)
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
            throw SemanticException("Type '$name' already declared", td.identifier.token.span.line)
        }
        val resolved = analyzeType(td.type)
        val typeNode = if (td.type !is DeclaredType) {
            td.type
        } else {
            td.type.data!!.originalType
        }
        currentScope.declareType(name, typeNode, resolved)
    }

    private fun analyzeRoutineDeclaration(rd: RoutineDeclaration) {
        val header = rd.header
        val name = header.name.token.lexeme
        if (globalScope.isDeclaredInCurrentScope(name)) {
            throw SemanticException("Routine '$name' already declared", header.name.token.span.line)
        }

        val params = rd.header.parameters.parameters.withIndex().map { (index, pd) ->
            analyzeType(pd.type, isLastParamPosInFunctionArgs = (index + 1 == rd.header.parameters.parameters.size))
        }
        rd.data = RoutineDeclarationSemanticData(isVariadic = false)
        if (params.isNotEmpty()) {
            val lastParam = params.last()
            if (lastParam is SemanticType.Array && lastParam.size == null) {
                if (params.size < 2 || params[params.size - 2] !is SemanticType.Integer) {
                    throw SemanticException("variadic")
                }
                rd.data = RoutineDeclarationSemanticData(isVariadic = true)
            }
        }
        val ret = header.returnType?.let { analyzeType(it) }

        globalScope.declareRoutine(name, RoutineSymbol(params, ret, rd, variadic = rd.data?.isVariadic == true))

        // Body
        rd.body?.let { body ->
            val saved = currentScope
            currentScope = SymbolTable(globalScope)
            // declare params in routine scope
            header.parameters.parameters.forEachIndexed { idx, pd ->
                currentScope.declareVariable(pd.name.token.lexeme, params[idx], null)
            }
            currentRoutine = name
            analyzeRoutineBody(body, ret, name, header.name.token.span.line)
            currentRoutine = null
            currentScope = saved
        }
    }

    // TODO: handle all return paths
    private fun analyzeRoutineBody(
        rb: RoutineBody,
        expectedReturn: SemanticType?,
        routineName: String,
        routineLine0: Int
    ) {
        when (rb) {
            is FullRoutineBody -> analyzeBody(rb.body)
            is SingleExpressionBody -> {
                val (t, _) = analyzeExpression(rb.expression)
                if (expectedReturn == null || !assignable(expectedReturn, t)) {
                    throw SemanticException("Wrong return type for routine '$routineName'", routineLine0)
                }
            }
        }
    }

    private fun analyzeVariableDeclaration(vd: VariableDeclaration) {
        when (vd) {
            is VariableDeclarationWithType -> {
                val name = vd.identifier.token.lexeme
                if (currentScope.isDeclaredInCurrentScope(name)) {
                    throw SemanticException(
                        "Variable '$name' already declared in this scope",
                        vd.identifier.token.span.line
                    )
                }
                val declared = analyzeType(vd.type)
                val cv = vd.initialValue?.let { iv ->
                    val (t, c) = analyzeExpression(iv)
                    if (!assignable(declared, t)) {
                        throw SemanticException("Cannot initialize '$name' with incompatible type", vd.identifier.token.span.line)
                    }
                    c
                }
                currentScope.declareVariable(name, declared, cv)
            }

            is VariableDeclarationNoType -> {
                val name = vd.identifier.token.lexeme
                if (currentScope.isDeclaredInCurrentScope(name)) {
                    throw SemanticException(
                        "Variable '$name' already declared in this scope",
                        vd.identifier.token.span.line
                    )
                }
                val (t, cv) = analyzeExpression(vd.initialValue)
                currentScope.declareVariable(name, t, cv)
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
            is RoutineCall -> analyzeRoutineCall(s)
            is WhileLoop -> analyzeWhileLoop(s)
            is ForLoop -> analyzeForLoop(s)
            is IfStatement -> analyzeIfStatement(s)
            is PrintStatement -> analyzePrintStatement(s)
            is ReturnStatement -> analyzeReturnStatement(s)
        }
    }

    private fun analyzeReturnStatement(s: ReturnStatement) {
        val sym = globalScope.lookupRoutine(currentRoutine!!)!!
        if (s.expression == null) {
            if (sym.returnType != null) {
                throw SemanticException()
            }
            return
        }
        val (t, _) = analyzeExpression(s.expression)
        if (sym.returnType == null || !assignable(sym.returnType, t)) {
            throw SemanticException()
        }
    }

    private fun analyzeAssignment(a: Assignment) {
        val lt = analyzeModifiablePrimary(a.modifiablePrimary)
        val (rt, _) = analyzeExpression(a.expression)
        if (!assignable(lt.first, rt)) {
            throw SemanticException("Type mismatch in assignment", a.modifiablePrimary.variable.token.span.line)
        }
    }

    private fun analyzeRoutineCall(call: RoutineCall): SemanticType? {
        // analyze children (arguments)
        call.arguments.forEach { analyzeExpression(it.expression) }

        val name = call.routineName.token.lexeme
        val sym = currentScope.lookupRoutine(name) ?: throw SemanticException(
            "Call to undeclared routine '$name'",
            call.routineName.token.span.line
        )
        if (sym.variadic) {
            for (i in 0 until sym.parameterTypes.size - 1) {
                val at = analyzeExpression(call.arguments[i].expression).first
                val pt = sym.parameterTypes[i]
                if (!assignable(pt, at)) {
                    throw SemanticException(
                        "Argument ${i + 1} type mismatch for '$name'",
                        call.routineName.token.span.line
                    )
                }
            }
            for (i in sym.parameterTypes.size - 1 until call.arguments.size) {
                val at = analyzeExpression(call.arguments[i].expression).first
                val pt = (sym.parameterTypes.last() as SemanticType.Array).elementType
                if (!assignable(pt, at)) {
                    throw SemanticException(
                        "Argument ${i + 1} type mismatch for '$name'",
                        call.routineName.token.span.line
                    )
                }
            }
        } else {
            if (call.arguments.size != sym.parameterTypes.size) {
                throw SemanticException("Wrong number of arguments for '$name'", call.routineName.token.span.line)
            }
            for (i in 0 until call.arguments.size) {
                val at = analyzeExpression(call.arguments[i].expression).first
                val pt = sym.parameterTypes[i]
                if (!assignable(pt, at)) {
                    throw SemanticException(
                        "Argument ${i + 1} type mismatch for '$name'",
                        call.routineName.token.span.line
                    )
                }
            }
        }
        return sym.returnType
    }

    private fun analyzeWhileLoop(w: WhileLoop) {
        val (t, cv) = analyzeExpression(w.condition)
        if (t !is SemanticType.Boolean) {
            throw SemanticException()
        }
        val saved = currentScope
        currentScope = SymbolTable(saved)
        analyzeBody(w.body)
        currentScope = saved
    }

    private fun analyzeForLoop(f: ForLoop) {
        val saved = currentScope
        currentScope = SymbolTable(saved)
        currentScope.declareVariable(f.loopVariable.token.lexeme, SemanticType.Integer, null)

        analyzeRange(f.range)
        analyzeBody(f.body)
        currentScope = saved
    }

    private fun analyzeIfStatement(i: IfStatement) {
        val (t, _) = analyzeExpression(i.condition)
        if (t !is SemanticType.Boolean) {
            throw SemanticException("Condition expression must be boolean type but it is $t in $i")
        }

        var saved = currentScope
        currentScope = SymbolTable(saved)
        analyzeBody(i.thenBody)
        currentScope = saved
        i.elseBody?.let {
            saved = currentScope
            currentScope = SymbolTable(saved)
            analyzeBody(it)
            currentScope = saved
        }
    }

    private fun analyzePrintStatement(p: PrintStatement) {
        analyzeExpression(p.expression)
        p.rest.forEach { analyzeExpression(it) }
    }

    // ---------------- Expressions ----------------

    private fun analyzeExpression(e: Expression): Pair<SemanticType, CompileTimeValue?> {
        var (t, c) = analyzeRelation(e.relation)
        e.rest?.forEach { (op, r) ->
            val (rt, rc) = analyzeRelation(r)
            if (t !is SemanticType.Boolean || rt !is SemanticType.Boolean) {
                throw SemanticException()
            }
            val out = if (c != null && rc != null) {
                val a = (c as CompileTimeBoolean).value
                val b = (rc as CompileTimeBoolean).value
                val v = when (op) {
                    ExpressionOperator.AND -> a && b
                    ExpressionOperator.OR -> a || b
                    ExpressionOperator.XOR -> (a && !b) || (!a && b)
                }
                CompileTimeBoolean(v)
            } else null
            t = SemanticType.Boolean
            c = out
        }
        if (c != null) {
            e.data = ExpressionSemanticData(c)
        }
        return t to c
    }

    private fun analyzeRelation(r: Relation): Pair<SemanticType, CompileTimeValue?> {
        val (lt, lc) = analyzeSimple(r.simple)
        if (r.comparison != null) {
            val (op, s) = r.comparison
            val (_, rc) = analyzeSimple(s)
            val out = if (lc != null && rc != null) {
                val res = when (op) {
                    RelationOperator.LT -> compare(lc, rc) < 0
                    RelationOperator.LE -> compare(lc, rc) <= 0
                    RelationOperator.GT -> compare(lc, rc) > 0
                    RelationOperator.GE -> compare(lc, rc) >= 0
                    RelationOperator.EQ -> equalConst(lc, rc)
                    RelationOperator.NEQ -> !equalConst(lc, rc)
                }
                CompileTimeBoolean(res)
            } else null
            return SemanticType.Boolean to out
        } else {
            return lt to lc
        }
    }

    private fun analyzeSimple(s: Simple): Pair<SemanticType, CompileTimeValue?> {
        var (t, c) = analyzeFactor(s.factor)
        s.rest?.forEach { (op, f) ->
            val (rt, rc) = analyzeFactor(f)
            val outType = arithmeticWidenRealOrInt(t, rt)
            val outConst = if (c != null && rc != null) {
                when (op) {
                    SimpleOperator.PLUS -> addConst(c, rc)
                    SimpleOperator.MINUS -> subConst(c, rc)
                }
            } else null
            t = outType
            c = outConst
        }
        return t to c
    }

    private fun analyzeFactor(f: Factor): Pair<SemanticType, CompileTimeValue?> {
        var (t, c) = analyzeSummand(f.summand)
        f.rest?.forEach { (op, s) ->
            val (rt, rc) = analyzeSummand(s)
            val outType = when (op) {
                FactorOperator.PRODUCT -> arithmeticWidenRealOrInt(t, rt)
                FactorOperator.DIVISION -> arithmeticWidenRealOrInt(t, rt)
                FactorOperator.MODULO -> {
                    if (t !is SemanticType.Integer || rt !is SemanticType.Integer) {
                        throw SemanticException()
                    }
                    SemanticType.Integer
                }
            }
            val outConst = if (c != null && rc != null) {
                when (op) {
                    FactorOperator.PRODUCT -> mulConst(c, rc)
                    FactorOperator.DIVISION -> divConst(c, rc)
                    FactorOperator.MODULO -> modConst(c as CompileTimeInteger, rc as CompileTimeInteger)
                }
            } else null
            t = outType
            c = outConst
        }
        return t to c
    }

    private fun analyzeSummand(s: Summand): Pair<SemanticType, CompileTimeValue?> =
        when (s) {
            is ExpressionInParenthesis -> analyzeExpressionInParenthesis(s)
            is Primary -> analyzePrimary(s)
        }

    private fun analyzeExpressionInParenthesis(ep: ExpressionInParenthesis): Pair<SemanticType, CompileTimeValue?> =
        analyzeExpression(ep.expression)

    private fun analyzePrimary(p: Primary): Pair<SemanticType, CompileTimeValue?> =
        when (p) {
            is UnaryInteger -> SemanticType.Integer to analyzeUnaryInteger(p)
            is UnaryReal -> SemanticType.Real to analyzeUnaryReal(p)
            is UnaryModifiablePrimary -> analyzeUnaryModifiablePrimary(p)
            is BooleanLiteral -> SemanticType.Boolean to analyzeBooleanLiteral(p)
            is RoutineCall -> {
                val t = analyzeRoutineCall(p) ?: throw SemanticException(
                    "Using a non-return routine in an expression",
                    p.routineName.token.span.line
                )
                t to null
            }
        }

    private fun analyzeUnaryInteger(ui: UnaryInteger): CompileTimeInteger {
        val rawStr = ui.integerLiteral.token.lexeme
        val raw: Int = rawStr.toIntOrNull() ?: throw SemanticException(
            "Integer literal out of range",
            ui.integerLiteral.token.span.line
        )
        val sRaw: Int = when (ui.unaryOperator) {
            null -> raw
            is UnarySign -> { if (ui.unaryOperator == UnarySign.MINUS) -raw else raw }
            is UnaryNot -> { (raw == 0).toInt() }
        }
        ui.data = UnaryIntegerSemanticData(sRaw)
        return CompileTimeInteger(sRaw)
    }

    private fun analyzeUnaryReal(ur: UnaryReal): CompileTimeDouble {
        val raw = ur.realLiteral.token.lexeme.toDoubleOrNull() ?: throw SemanticException(
            "Invalid real literal",
            ur.realLiteral.token.span.line
        )
        val v = if (ur.unaryRealOperator == UnarySign.MINUS) -raw else raw
        ur.data = UnaryRealSemanticData(v)
        return CompileTimeDouble(v)
    }

    private fun analyzeUnaryModifiablePrimary(ump: UnaryModifiablePrimary): Pair<SemanticType, CompileTimeValue?> {
        val (bt, bcv) = analyzeModifiablePrimary(ump.modifiablePrimary)
        return when (ump.unaryOperator) {
            null -> bt to bcv
            is UnaryNot -> {
                if (bcv == null) {
                    check(bt is SemanticType.Integer || bt is SemanticType.Boolean)
                    bt to null
                } else if (bt is SemanticType.Integer) {
                    check(bcv is CompileTimeInteger)
                    SemanticType.Integer to CompileTimeInteger((bcv.value == 0).toInt())
                } else if (bt is SemanticType.Boolean) {
                    check(bcv is CompileTimeBoolean)
                    SemanticType.Boolean to CompileTimeBoolean(!bcv.value)
                } else {
                    throw SemanticException()
                }
            }

            is UnarySign -> {
                if (!isPrimitive(bt)) {
                    throw SemanticException(
                        "Unary sign cannot be applied to non-primitive value",
                        ump.modifiablePrimary.variable.token.span.line
                    )
                } else {
                    when (bt) {
                        SemanticType.Real -> SemanticType.Real to null
                        SemanticType.Integer -> SemanticType.Integer to null
                        else -> throw SemanticException()
                    }
                }
            }
        }
    }

    private fun analyzeBooleanLiteral(bl: BooleanLiteral): CompileTimeBoolean {
        val v = bl == BooleanLiteral.TRUE
        return CompileTimeBoolean(v)
    }

    private fun analyzeModifiablePrimary(mp: ModifiablePrimary): Pair<SemanticType, CompileTimeValue?> {
        var (t, cv) = analyzeVariable(mp.variable)
        if (mp.accessors.isEmpty()) {
            mp.data = ModifiablePrimarySemanticData(cv)
            return t to cv
        }
        mp.accessors.forEach { t = analyzeAccessor(t, it) }
        return t to null
    }

    private fun analyzeVariable(id: Identifier): Pair<SemanticType, CompileTimeValue?> {
        val name = id.token.lexeme
        val t = currentScope.lookupVariable(name) ?: throw SemanticException(
            "Undeclared variable '$name'",
            id.token.span.line
        )
        return t.semanticType to t.compileTimeValue
    }

    private fun analyzeAccessor(prev: SemanticType, acc: Accessor): SemanticType =
        when (acc) {
            is FieldAccessor -> analyzeFieldAccessor(prev, acc)
            is ArrayAccessor -> analyzeArrayAccessor(prev, acc)
        }

    private fun analyzeFieldAccessor(prev: SemanticType, acc: FieldAccessor): SemanticType {
        if (prev is SemanticType.Record) {
            val f = acc.identifier.token.lexeme
            val ft = prev.fields.find { it.name == f } ?: throw SemanticException("No such field '$f'", acc.identifier.token.span.line)
            return ft.type
        }
        throw SemanticException("Field access on non-record value", acc.identifier.token.span.line)
    }

    private fun analyzeArrayAccessor(prev: SemanticType, acc: ArrayAccessor): SemanticType {
        val (t, cv) = analyzeExpression(acc.expression)
        if (t !is SemanticType.Integer) {
            throw SemanticException()
        }
        if (cv != null && cv !is CompileTimeInteger) {
            throw SemanticException()
        }
        if (prev is SemanticType.Array) {
            return prev.elementType
        }
        throw SemanticException()
    }

    // ---------------- Types ----------------

    private fun analyzeType(t: Type, isLastParamPosInFunctionArgs: Boolean = false): SemanticType {
        val resolvedType = when (t) {
            PrimitiveType.INTEGER -> SemanticType.Integer
            PrimitiveType.REAL -> SemanticType.Real
            PrimitiveType.BOOLEAN -> SemanticType.Boolean

            is DeclaredType -> {
                val name = t.identifier.token.lexeme
                val td = currentScope.lookupType(name) ?: run {
                    throw SemanticException("Unknown type '$name'", t.identifier.token.span.line)
                }
                t.data = DeclaredTypeSemanticData(td.underlyingType)
                td.semanticType
            }

            is ArrayType -> analyzeArrayType(t, isLastParamPosInFunctionArgs)
            is RecordType -> analyzeRecordType(t)
        }
        return resolvedType
    }

    private fun analyzeArrayType(at: ArrayType, isLastParamPosInFunctionArgs: Boolean): SemanticType.Array {
        val elem = analyzeType(at.type)
        val const = at.expressionInBrackets?.let { analyzeExpression(it).second }
        if (at.expressionInBrackets != null && const !is CompileTimeInteger) {
            throw SemanticException()
        }
        if (!isLastParamPosInFunctionArgs && at.expressionInBrackets == null) {
            throw SemanticException()
        }
        val size = const?.let { (it as CompileTimeInteger).value }
        return SemanticType.Array(size, elem)
    }

    private fun analyzeRecordType(rt: RecordType): SemanticType.Record {
        val fmap = mutableMapOf<String, SemanticType>()
        val fields: List<SemanticType.Record.RecordField> = rt.declarations.map { d ->
            when (d) {
                is VariableDeclarationWithType -> {
                    val fname = d.identifier.token.lexeme
                    if (fmap.containsKey(fname)) {
                        throw SemanticException("Duplicate record field '$fname'", d.identifier.token.span.line)
                    } else {
                        val ftype = analyzeType(d.type)
                        d.initialValue?.let { analyzeExpression(it) }
                        fmap[fname] = ftype
                        SemanticType.Record.RecordField(fname, ftype)
                    }
                }

                is VariableDeclarationNoType -> {
                    val fname = d.identifier.token.lexeme
                    if (fmap.containsKey(fname)) {
                        throw SemanticException("Duplicate record field '$fname'", d.identifier.token.span.line)
                    } else {
                        val (ftype, _) = analyzeExpression(d.initialValue)
                        fmap[fname] = ftype
                        SemanticType.Record.RecordField(fname, ftype)
                    }
                }
            }
        }
        return SemanticType.Record(fields = fields)
    }

    private fun analyzeRange(r: Range) {
        val (bt, _) = analyzeExpression(r.begin)
        if (bt != SemanticType.Integer) {
            throw SemanticException("For-loop begin bound must be integer: $r")
        }
        r.end?.let { e ->
            val (et, _) = analyzeExpression(e)
            if (et != SemanticType.Integer) {
                throw SemanticException("For-loop end bound must be integer: $r")
            }
        }

    }

    // Various stuff for primitive types

    private fun assignable(expected: SemanticType, actual: SemanticType): Boolean {
        if (isPrimitive(expected) && isPrimitive(actual)) {
            return !(expected is SemanticType.Boolean && actual is SemanticType.Real)
        }

        if (expected is SemanticType.Array && actual is SemanticType.Array) {
            return expected.size == actual.size && assignable(expected.elementType, actual.elementType)
        }

        if (expected is SemanticType.Record && actual is SemanticType.Record) {
            if (expected.fields != actual.fields) return false
        }

        return false
    }

    private fun arithmeticWidenRealOrInt(a: SemanticType, b: SemanticType): SemanticType {
        if (a !is SemanticType.SemanticPrimitiveType || b !is SemanticType.SemanticPrimitiveType) {
            throw SemanticException()
        }
        if (a is SemanticType.Boolean || b is SemanticType.Boolean) {
            throw SemanticException()
        }

        if (a == SemanticType.Real || b == SemanticType.Real) return SemanticType.Real
        return SemanticType.Integer
    }

    private fun isPrimitive(t: SemanticType) = t is SemanticType.SemanticPrimitiveType

    private fun toBool(v: CompileTimeValue): Boolean = when (v) {
        is CompileTimeBoolean -> v.value
        is CompileTimeInteger -> v.value != 0
        is CompileTimeDouble -> v.value != 0.0
    }

    private fun toInteger(v: CompileTimeValue): Int = when (v) {
        is CompileTimeBoolean -> if (v.value) 1 else 0
        is CompileTimeInteger -> v.value
        is CompileTimeDouble -> v.value.toInt()
    }

    private fun toDouble(v: CompileTimeValue): Double = when (v) {
        is CompileTimeBoolean -> if (v.value) 1.0 else 0.0
        is CompileTimeInteger -> v.value.toDouble()
        is CompileTimeDouble -> v.value
    }

    private fun equalConst(a: CompileTimeValue, b: CompileTimeValue): Boolean {
        return when {
            a is CompileTimeDouble || b is CompileTimeDouble -> toDouble(a) == toDouble(b)
            a is CompileTimeInteger || b is CompileTimeInteger -> toInteger(a) == toInteger(b)
            else -> (a as CompileTimeBoolean).value == (b as CompileTimeBoolean).value
        }
    }

    private fun compare(a: CompileTimeValue, b: CompileTimeValue): Int {
        return when {
            a is CompileTimeDouble || b is CompileTimeDouble -> toDouble(a).compareTo(toDouble(b))
            else -> toInteger(a).compareTo(toInteger(b))
        }
    }

    private fun addConst(a: CompileTimeValue, b: CompileTimeValue): CompileTimeValue {
        if (a is CompileTimeBoolean || b is CompileTimeBoolean) {
            throw SemanticException()
        }

        return if (a is CompileTimeDouble || b is CompileTimeDouble) CompileTimeDouble(toDouble(a) + toDouble(b))
        else CompileTimeInteger(toInteger(a) + toInteger(b))
    }

    private fun subConst(a: CompileTimeValue, b: CompileTimeValue): CompileTimeValue {
        if (a is CompileTimeBoolean || b is CompileTimeBoolean) {
            throw SemanticException()
        }

        return if (a is CompileTimeDouble || b is CompileTimeDouble) CompileTimeDouble(toDouble(a) - toDouble(b))
        else CompileTimeInteger(toInteger(a) - toInteger(b))
    }

    private fun mulConst(a: CompileTimeValue, b: CompileTimeValue): CompileTimeValue {
        if (a is CompileTimeBoolean || b is CompileTimeBoolean) {
            throw SemanticException()
        }

        return if (a is CompileTimeDouble || b is CompileTimeDouble) CompileTimeDouble(toDouble(a) * toDouble(b))
        else CompileTimeInteger(toInteger(a) * toInteger(b))
    }

    private fun divConst(a: CompileTimeValue, b: CompileTimeValue): CompileTimeValue {
        if (a is CompileTimeBoolean || b is CompileTimeBoolean) {
            throw SemanticException()
        }

        return if (a is CompileTimeDouble || b is CompileTimeDouble) CompileTimeDouble(toDouble(a) / toDouble(b))
        else CompileTimeInteger(toInteger(a) / toInteger(b))
    }

    private fun modConst(a: CompileTimeInteger, b: CompileTimeInteger): CompileTimeInteger {
        return CompileTimeInteger(a.value % b.value)
    }
}
