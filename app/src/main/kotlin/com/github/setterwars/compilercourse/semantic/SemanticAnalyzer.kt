package com.github.setterwars.compilercourse.semantic

import com.github.setterwars.compilercourse.parser.nodes.*

class SemanticAnalyzer(private val mode: Mode = Mode.Permissive) {
    enum class Mode { Permissive, Strict }

    private val globalScope = SymbolTable(null)
    private var currentScope = globalScope
    private val errors = mutableListOf<SemanticError>()
    private val infoStore = SemanticInfoStore()

    // Type declarations registry and cache for resolved user types
    private val typeDecls = mutableMapOf<String, Type>()
    private val resolvedTypes = mutableMapOf<String, ResolvedType>()
    private val resolvingTypes = mutableSetOf<String>()

    fun analyze(program: Program): AnalysisResult {
        errors.clear()
        currentScope = globalScope
        typeDecls.clear()
        resolvedTypes.clear()
        resolvingTypes.clear()

        // First pass: collect all type and routine declarations
        program.declarations.forEach { declaration ->
            when (declaration) {
                is TypeDeclaration -> declareType(declaration)
                is RoutineDeclaration -> declareRoutine(declaration)
                is VariableDeclaration -> {} // Skip for first pass
            }
        }

        // Second pass: analyze everything
        program.declarations.forEach { declaration ->
            analyzeDeclaration(declaration)
        }

        // Pretty print AST with semantic annotations
        val pretty = PrettyAstPrinter(infoStore).print(program)
        println(pretty)

        return AnalysisResult(errors.toList())
    }

    private fun error(message: String, line: Int) {
        if (mode == Mode.Strict) {
            errors.add(SemanticError(message, line + 1))
        }
    }

    private fun declareType(declaration: TypeDeclaration) {
        val name = declaration.identifier.token.lexeme
        if (currentScope.isDeclaredInCurrentScope(name)) {
            error("Type '$name' is already declared", declaration.identifier.token.span.line)
        } else {
            // Mark name as used in this scope to catch duplicates
            currentScope.declareType(name, declaration.type)
            typeDecls[name] = declaration.type
        }
    }

    private fun declareRoutine(declaration: RoutineDeclaration) {
        val name = declaration.header.name.token.lexeme
        if (currentScope.isDeclaredInCurrentScope(name)) {
            error("Routine '$name' is already declared", declaration.header.name.token.span.line)
        } else {
            val paramTypes = declaration.header.parameters.parameters.map { it.type }
            val returnType = declaration.header.returnType
            currentScope.declareRoutine(name, RoutineSymbol(paramTypes, returnType, declaration))
        }
    }

    private fun analyzeDeclaration(declaration: Declaration) {
        when (declaration) {
            is VariableDeclaration -> analyzeVariableDeclaration(declaration)
            is TypeDeclaration -> analyzeTypeDeclaration(declaration)
            is RoutineDeclaration -> analyzeRoutineDeclaration(declaration)
        }
    }

    private fun analyzeVariableDeclaration(declaration: VariableDeclaration) {
        val name = when (declaration) {
            is VariableDeclarationWithType -> declaration.identifier.token.lexeme
            is VariableDeclarationNoType -> declaration.identifier.token.lexeme
        }

        val line = when (declaration) {
            is VariableDeclarationWithType -> declaration.identifier.token.span.line
            is VariableDeclarationNoType -> declaration.identifier.token.span.line
        }

        if (currentScope.isDeclaredInCurrentScope(name)) {
            error("Variable '$name' is already declared in this scope", line)
            return
        }

        when (declaration) {
            is VariableDeclarationWithType -> {
                val declaredType = resolveType(declaration.type) ?: ResolvedType.Unknown
                if (declaration.initialValue != null) {
                    val exprType = inferExpressionType(declaration.initialValue)
                    if (!typesCompatible(declaredType, exprType)) {
                        error("Type mismatch in variable initialization: expected ${typeToString(declaredType)}, got ${typeToString(exprType)}", line)
                    }
                }
                currentScope.declareVariable(name, declaredType)
            }
            is VariableDeclarationNoType -> {
                val exprType = inferExpressionType(declaration.initialValue)
                currentScope.declareVariable(name, exprType ?: ResolvedType.Unknown)
            }
        }
    }

    private fun analyzeTypeDeclaration(declaration: TypeDeclaration) {
        val name = declaration.identifier.token.lexeme
        val resolvedType = resolveType(declaration.type)
        if (resolvedType == null) {
            error("Invalid type in type declaration", declaration.identifier.token.span.line)
        } else {
            // Cache resolved type for future lookups
            resolvedTypes[name] = resolvedType
        }
    }

    private fun analyzeRoutineDeclaration(declaration: RoutineDeclaration) {
        // Create new scope for routine
        val routineScope = SymbolTable(currentScope)
        currentScope = routineScope

        // Declare parameters in routine scope
        declaration.header.parameters.parameters.forEach { param ->
            val paramType = resolveType(param.type) ?: ResolvedType.Unknown
            currentScope.declareVariable(param.name.token.lexeme, paramType)
        }

        // Analyze routine body
        when (val body = declaration.body) {
            is FullRoutineBody -> analyzeBody(body.body, declaration.header.returnType)
            is SingleExpressionBody -> {
                val exprType = inferExpressionType(body.expression)
                val expectedType = declaration.header.returnType?.let { resolveType(it) } ?: ResolvedType.Unknown
                if (!typesCompatible(expectedType, exprType)) {
                    error("Wrong return type: expected ${typeToString(expectedType)}, got ${typeToString(exprType)}", declaration.header.name.token.span.line)
                }
            }
            null -> {} // Forward declaration
        }

        // Restore previous scope
        currentScope = currentScope.parent!!
    }

    private fun analyzeBody(body: Body, expectedReturnType: Type?) {
        body.bodyElements.forEach { element ->
            when (element) {
                is SimpleDeclaration -> analyzeDeclaration(element)
                is Statement -> analyzeStatement(element, expectedReturnType)
            }
        }
    }

    private fun analyzeStatement(statement: Statement, expectedReturnType: Type?) {
        when (statement) {
            is Assignment -> analyzeAssignment(statement)
            is RoutineCall -> analyzeRoutineCall(statement)
            is WhileLoop -> analyzeWhileLoop(statement, expectedReturnType)
            is ForLoop -> analyzeForLoop(statement, expectedReturnType)
            is IfStatement -> analyzeIfStatement(statement, expectedReturnType)
            is PrintStatement -> analyzePrintStatement(statement)
        }
    }

    private fun analyzeAssignment(assignment: Assignment) {
        val targetType = inferModifiablePrimaryType(assignment.modifiablePrimary)
        val exprType = inferExpressionType(assignment.expression)

        if (!typesCompatible(targetType, exprType)) {
            error("Type mismatch in assignment: cannot assign ${typeToString(exprType)} to ${typeToString(targetType)}", assignment.modifiablePrimary.variable.token.span.line)
        }
    }

    private fun analyzeRoutineCall(call: RoutineCall): ResolvedType? {
        val routineName = call.routineName.token.lexeme
        val routine = currentScope.lookupRoutine(routineName)

        if (routine == null) {
            // Permissive: unknown routine returns Unknown; Strict: error
            error("Routine '$routineName' is not declared", call.routineName.token.span.line)
            return ResolvedType.Unknown
        }

        // Check argument count
        if (call.arguments.size != routine.parameterTypes.size) {
            error("Wrong number of arguments for routine '$routineName': expected ${routine.parameterTypes.size}, got ${call.arguments.size}", call.routineName.token.span.line)
            return routine.returnType?.let { resolveType(it) } ?: ResolvedType.Unknown
        }

        // Check argument types
        call.arguments.forEachIndexed { index, arg ->
            val argType = inferExpressionType(arg.expression)
            val paramType = resolveType(routine.parameterTypes[index]) ?: ResolvedType.Unknown
            if (!typesCompatible(paramType, argType)) {
                error("Type mismatch in argument ${index + 1} of routine '$routineName': expected ${typeToString(paramType)}, got ${typeToString(argType)}", call.routineName.token.span.line)
            }
        }

        val ret = routine.returnType?.let { resolveType(it) } ?: ResolvedType.Unknown
        infoStore.setRoutineCallType(call, ret)
        return ret
    }

    private fun analyzeWhileLoop(loop: WhileLoop, expectedReturnType: Type?) {
        val condType = inferExpressionType(loop.condition)
        if (!(condType == ResolvedType.Boolean || condType == ResolvedType.Unknown)) {
            error("While loop condition must be boolean", exprLine(loop.condition))
        }

        // Create new scope for loop body
        val loopScope = SymbolTable(currentScope)
        currentScope = loopScope
        analyzeBody(loop.body, expectedReturnType)
        currentScope = currentScope.parent!!
    }

    private fun analyzeForLoop(loop: ForLoop, expectedReturnType: Type?) {
        // Create new scope for loop
        val loopScope = SymbolTable(currentScope)
        currentScope = loopScope

        // Declare loop variable as integer (or Unknown in permissive mode if needed)
        currentScope.declareVariable(loop.loopVariable.token.lexeme, ResolvedType.Integer)

        // Check range types
        val beginType = inferExpressionType(loop.range.begin)
        if (!(beginType == ResolvedType.Integer || beginType == ResolvedType.Unknown)) {
            error("For loop range begin must be integer", loop.loopVariable.token.span.line)
        }

        val beginConst = extractIntegerLiteral(loop.range.begin)
        val endConst = loop.range.end?.let { extractIntegerLiteral(it) }
        if (loop.range.end != null) {
            val endType = inferExpressionType(loop.range.end)
            if (!(endType == ResolvedType.Integer || endType == ResolvedType.Unknown)) {
                error("For loop range end must be integer", loop.loopVariable.token.span.line)
            }
        }
        // If both are constants and no reverse, check bounds order (strict only)
        if (mode == Mode.Strict && !loop.reverse && beginConst != null && endConst != null) {
            if (beginConst > endConst) {
                error("Invalid for-loop range: $beginConst .. $endConst", loop.loopVariable.token.span.line)
            }
        }

        analyzeBody(loop.body, expectedReturnType)
        currentScope = currentScope.parent!!
    }

    private fun analyzeIfStatement(statement: IfStatement, expectedReturnType: Type?) {
        val condType = inferExpressionType(statement.condition)
        if (!(condType == ResolvedType.Boolean || condType == ResolvedType.Unknown)) {
            error("If statement condition must be boolean", exprLine(statement.condition))
        }

        // Analyze then body
        val thenScope = SymbolTable(currentScope)
        currentScope = thenScope
        analyzeBody(statement.thenBody, expectedReturnType)
        currentScope = currentScope.parent!!

        // Analyze else body if present
        if (statement.elseBody != null) {
            val elseScope = SymbolTable(currentScope)
            currentScope = elseScope
            analyzeBody(statement.elseBody, expectedReturnType)
            currentScope = currentScope.parent!!
        }
    }

    private fun analyzePrintStatement(statement: PrintStatement) {
        inferExpressionType(statement.expression)
        statement.rest.forEach { inferExpressionType(it) }
    }

    private fun inferModifiablePrimaryType(modifiable: ModifiablePrimary): ResolvedType? {
        val baseName = modifiable.variable.token.lexeme
        var currentType = currentScope.lookupVariable(baseName)

        if (currentType == null) {
            // Permissive: implicitly declare unknown variable with Unknown type
            error("Variable '$baseName' is not declared", modifiable.variable.token.span.line)
            currentScope.declareVariable(baseName, ResolvedType.Unknown)
            currentType = ResolvedType.Unknown
        }

        modifiable.accessors?.forEach { accessor ->
            when (accessor) {
                is FieldAccessor -> {
                    when (currentType) {
                        is ResolvedType.Record -> {
                            val fieldName = accessor.identifier.token.lexeme
                            currentType = currentType.fields[fieldName]
                            if (currentType == null) {
                                error("Record field '$fieldName' does not exist", accessor.identifier.token.span.line)
                                currentType = ResolvedType.Unknown
                            }
                        }
                        is ResolvedType.Unknown -> {
                            // Treat unknown as structural; field access remains Unknown
                            currentType = ResolvedType.Unknown
                        }
                        else -> {
                            error("Cannot access field of non-record type", accessor.identifier.token.span.line)
                            currentType = ResolvedType.Unknown
                        }
                    }
                }
                is ArrayAccessor -> {
                    when (currentType) {
                        is ResolvedType.Array -> {
                            val indexType = inferExpressionType(accessor.expression)
                            if (!(indexType == ResolvedType.Integer || indexType == ResolvedType.Unknown)) {
                                error("Array index must be integer", modifiable.variable.token.span.line)
                            }
                            val idxConst = evalIntConstant(accessor.expression)
                            if (mode == Mode.Strict && idxConst == null) {
                                error("Array index must be a compile-time integer constant", modifiable.variable.token.span.line)
                            }
                            if (idxConst != null && currentType.size != null) {
                                if (idxConst < 1 || idxConst > currentType.size) {
                                    error("Array index out of bounds: $idxConst not in 1..${currentType.size}", modifiable.variable.token.span.line)
                                }
                            }
                            currentType = currentType.elementType
                        }
                        is ResolvedType.Unknown -> {
                            // Unknown indexed remains Unknown
                            currentType = ResolvedType.Unknown
                        }
                        else -> {
                            error("Cannot index non-array type", modifiable.variable.token.span.line)
                            currentType = ResolvedType.Unknown
                        }
                    }
                }
            }
        }

        if (currentType != null) {
            infoStore.setModifiablePrimaryType(modifiable, currentType)
        }
        return currentType
    }

    private fun inferExpressionType(expression: Expression): ResolvedType? {
        var resultType = inferRelationType(expression.relation)

        expression.rest?.forEach { (_, relation) ->
            val rightType = inferRelationType(relation)
            if (!((resultType == ResolvedType.Boolean || resultType == ResolvedType.Unknown) && (rightType == ResolvedType.Boolean || rightType == ResolvedType.Unknown))) {
                error("Logical operators (and, or, xor) require boolean operands", exprLine(expression))
            }
            // result is boolean if both are boolean; otherwise unknown
            resultType = if (resultType == ResolvedType.Boolean && rightType == ResolvedType.Boolean) ResolvedType.Boolean else ResolvedType.Unknown
        }

        resultType?.let { infoStore.setExpressionType(expression, it) }
        return resultType
    }

    private fun inferRelationType(relation: Relation): ResolvedType? {
        val leftType = inferSimpleType(relation.simple)

        if (relation.comparison != null) {
            val (_, right) = relation.comparison
            val rightType = inferSimpleType(right)

            if (!typesCompatible(leftType, rightType)) {
                error("Cannot compare incompatible types: ${typeToString(leftType)} and ${typeToString(rightType)}", relationLine(relation))
            }

            val t = ResolvedType.Boolean
            val compileTime = computeRelationConst(relation)
            infoStore.setRelationInfo(relation, t, compileTime)
            return t
        }

        leftType?.let { infoStore.setRelationInfo(relation, it, null) }
        return leftType
    }

    private fun inferSimpleType(simple: Simple): ResolvedType? {
        var resultType = inferFactorType(simple.factor)

        simple.rest?.forEach { (_, factor) ->
            val rightType = inferFactorType(factor)
            resultType = combineNumericTypes(resultType, rightType)
        }

        resultType?.let { infoStore.setSimpleType(simple, it) }
        return resultType
    }

    private fun inferFactorType(factor: Factor): ResolvedType? {
        var resultType = inferSummandType(factor.summand)

        factor.rest?.forEach { (_, summand) ->
            val rightType = inferSummandType(summand)
            resultType = combineNumericTypes(resultType, rightType)
        }

        resultType?.let { infoStore.setFactorType(factor, it) }
        return resultType
    }

    private fun inferSummandType(summand: Summand): ResolvedType? {
        return when (summand) {
            is ExpressionInParenthesis -> inferExpressionType(summand.expression)
            is Primary -> inferPrimaryType(summand)
            else -> ResolvedType.Unknown
        }
    }

    private fun inferPrimaryType(primary: Primary): ResolvedType? {
        return when (primary) {
            is UnaryInteger -> {
                val sign = primary.unaryOperator
                val raw = primary.integerLiteral.token.lexeme.toIntOrNull()
                if (raw != null) {
                    val v = if (sign == UnarySign.MINUS) -raw else raw
                    infoStore.setIntegerLiteralValue(primary.integerLiteral, raw)
                    infoStore.setUnaryIntegerValue(primary, v)
                }
                ResolvedType.Integer
            }
            is UnaryReal -> {
                val raw = primary.realLiteral.token.lexeme.toDoubleOrNull()
                if (raw != null) infoStore.setRealLiteralValue(primary.realLiteral, raw)
                ResolvedType.Real
            }
            is UnaryModifiablePrimary -> inferModifiablePrimaryType(primary.modifiablePrimary)
            is BooleanLiteral -> ResolvedType.Boolean
            is RoutineCall -> analyzeRoutineCall(primary)
            else -> ResolvedType.Unknown
        }
    }

    private fun resolveType(type: Type): ResolvedType? {
        return when (type) {
            PrimitiveType.INTEGER -> ResolvedType.Integer
            PrimitiveType.REAL -> ResolvedType.Real
            PrimitiveType.BOOLEAN -> ResolvedType.Boolean
            is DeclaredType -> {
                val name = type.identifier.token.lexeme
                // Check already resolved cache first
                resolvedTypes[name] ?: run {
                    val target = typeDecls[name]
                    if (target == null) {
                        error("Type '$name' is not declared", type.identifier.token.span.line)
                        null
                    } else {
                        if (name in resolvingTypes) {
                            error("Cyclic type definition for '$name'", type.identifier.token.span.line)
                            null
                        } else {
                            resolvingTypes.add(name)
                            val resolved = resolveType(target)
                            if (resolved != null) {
                                resolvedTypes[name] = resolved
                            }
                            resolvingTypes.remove(name)
                            resolved
                        }
                    }
                }
            }
            is ArrayType -> {
                val elementType = resolveType(type.type) ?: ResolvedType.Unknown
                val size = type.expressionInBrackets?.let { expr -> extractIntegerLiteral(expr) }
                ResolvedType.Array(elementType, size)
            }
            is RecordType -> {
                val fields = mutableMapOf<String, ResolvedType>()
                type.declarations.forEach { decl ->
                    when (decl) {
                        is VariableDeclarationWithType -> {
                            val fieldType = resolveType(decl.type) ?: ResolvedType.Unknown
                            val fieldName = decl.identifier.token.lexeme
                            if (fields.containsKey(fieldName)) {
                                error("Duplicate record field '$fieldName'", decl.identifier.token.span.line)
                            } else {
                                fields[fieldName] = fieldType
                            }
                        }
                        is VariableDeclarationNoType -> {
                            val fieldType = inferExpressionType(decl.initialValue) ?: ResolvedType.Unknown
                            val fieldName = decl.identifier.token.lexeme
                            if (fields.containsKey(fieldName)) {
                                error("Duplicate record field '$fieldName'", decl.identifier.token.span.line)
                            } else {
                                fields[fieldName] = fieldType
                            }
                        }
                    }
                }
                ResolvedType.Record(fields)
            }
        }
    }

    private fun extractIntegerLiteral(expression: Expression): Int? {
        val primary = expression.relation.simple.factor.summand as? UnaryInteger
        val sign = primary?.unaryOperator
        val value = primary?.integerLiteral?.token?.lexeme?.toIntOrNull()
        return if (value != null) {
            when (sign) {
                is UnarySign -> if (sign == UnarySign.MINUS) -value else value
                else -> value
            }
        } else null
    }

    private fun exprLine(expression: Expression): Int {
        // Try to find a representative token line inside expression
        val r = expression.relation
        return relationLine(r)
    }

    private fun relationLine(relation: Relation): Int {
        val s = relation.simple
        return simpleLine(s)
    }

    private fun simpleLine(simple: Simple): Int {
        val f = simple.factor
        return factorLine(f)
    }

    private fun factorLine(factor: Factor): Int {
        val s = factor.summand
        return when (s) {
            is ExpressionInParenthesis -> exprLine(s.expression)
            is UnaryInteger -> s.integerLiteral.token.span.line
            is UnaryReal -> s.realLiteral.token.span.line
            is UnaryModifiablePrimary -> s.modifiablePrimary.variable.token.span.line
            is BooleanLiteral -> 0
            is RoutineCall -> s.routineName.token.span.line
            else -> 0
        }
    }
    private fun evalIntConstant(expression: Expression): Int? {
        // Expression supports logical ops; indices must be pure arithmetic integer expression
        if (expression.rest != null && expression.rest.isNotEmpty()) return null
        return evalRelationIntConst(expression.relation)
    }

    private fun evalRelationIntConst(relation: Relation): Int? {
        // No comparisons allowed for an index constant
        if (relation.comparison != null) return null
        return evalSimpleIntConst(relation.simple)
    }

    private fun evalSimpleIntConst(simple: Simple): Int? {
        var acc = evalFactorIntConst(simple.factor) ?: return null
        simple.rest?.forEach { (op, factor) ->
            val rhs = evalFactorIntConst(factor) ?: return null
            acc = when (op) {
                SimpleOperator.PLUS -> acc + rhs
                SimpleOperator.MINUS -> acc - rhs
            }
        }
        return acc
    }

    private fun evalFactorIntConst(factor: Factor): Int? {
        var acc = evalSummandIntConst(factor.summand) ?: return null
        factor.rest?.forEach { (op, summand) ->
            val rhs = evalSummandIntConst(summand) ?: return null
            acc = when (op) {
                FactorOperator.PRODUCT -> acc * rhs
                FactorOperator.DIVISION -> if (rhs != 0) acc / rhs else return null
                FactorOperator.MODULO -> if (rhs != 0) acc % rhs else return null
            }
        }
        return acc
    }

    private fun evalSummandIntConst(summand: Summand): Int? {
        return when (summand) {
            is ExpressionInParenthesis -> evalIntConstant(summand.expression)
            is UnaryInteger -> {
                val raw = summand.integerLiteral.token.lexeme.toIntOrNull() ?: return null
                if (summand.unaryOperator == UnarySign.MINUS) -raw else raw
            }
            // Other primaries (real, modifiable, routine call, boolean) are not integer constants
            else -> null
        }
    }

    private fun computeRelationConst(relation: Relation): Boolean? {
        val (op, rightSimple) = relation.comparison ?: return null
        val left = relation.simple
        val lc = extractSimpleNumericConst(left)
        val rc = extractSimpleNumericConst(rightSimple)
        if (lc == null || rc == null) return null
        return when (op) {
            RelationOperator.LT -> lc < rc
            RelationOperator.LE -> lc <= rc
            RelationOperator.GT -> lc > rc
            RelationOperator.GE -> lc >= rc
            RelationOperator.EQ -> lc == rc
            RelationOperator.NEQ -> lc != rc
        }
    }

    private fun extractSimpleNumericConst(simple: Simple): Double? {
        if (simple.rest != null && simple.rest.isNotEmpty()) return null
        val factor = simple.factor
        if (factor.rest != null && factor.rest.isNotEmpty()) return null
        val summand = factor.summand
        return when (summand) {
            is UnaryInteger -> {
                val raw = summand.integerLiteral.token.lexeme.toIntOrNull() ?: return null
                val v = if (summand.unaryOperator == UnarySign.MINUS) -raw else raw
                v.toDouble()
            }
            is UnaryReal -> {
                val raw = summand.realLiteral.token.lexeme.toDoubleOrNull() ?: return null
                val v = if (summand.unaryRealOperator == UnarySign.MINUS) -raw else raw
                v
            }
            else -> null
        }
    }

    private fun combineNumericTypes(left: ResolvedType?, right: ResolvedType?): ResolvedType? {
        if (left == ResolvedType.Unknown || right == ResolvedType.Unknown) return ResolvedType.Unknown
        if (left == null || right == null) return ResolvedType.Unknown

        if (left == ResolvedType.Real || right == ResolvedType.Real) {
            if ((left == ResolvedType.Integer || left == ResolvedType.Real) && (right == ResolvedType.Integer || right == ResolvedType.Real)) {
                return ResolvedType.Real
            }
        }

        if (left == ResolvedType.Integer && right == ResolvedType.Integer) {
            return ResolvedType.Integer
        }

        error("Cannot perform arithmetic on non-numeric types", 0)
        return ResolvedType.Unknown
    }

    private fun typesCompatible(expected: ResolvedType?, actual: ResolvedType?): Boolean {
        if (expected == ResolvedType.Unknown || actual == ResolvedType.Unknown) return true
        if (expected == null || actual == null) return true
        if (expected == actual) return true

        // Integer can be assigned to Real
        if (expected == ResolvedType.Real && actual == ResolvedType.Integer) return true

        // Check structural compatibility for arrays and records
        if (expected is ResolvedType.Array && actual is ResolvedType.Array) {
            return typesCompatible(expected.elementType, actual.elementType)
        }

        return false
    }

    private fun typeToString(type: ResolvedType?): String {
        return when (type) {
            null -> "<unknown>"
            ResolvedType.Integer -> "integer"
            ResolvedType.Real -> "real"
            ResolvedType.Boolean -> "boolean"
            ResolvedType.Unknown -> "unknown"
            is ResolvedType.Array -> "array[${type.size ?: ""}] ${typeToString(type.elementType)}"
            is ResolvedType.Record -> "record"
            is ResolvedType.Placeholder -> type.name
        }
    }
}


data class AnalysisResult(
    val errors: List<SemanticError>
)


data class SemanticError(
    val message: String,
    val line: Int
) {
    override fun toString(): String = "Semantic error at line $line: $message"
}
