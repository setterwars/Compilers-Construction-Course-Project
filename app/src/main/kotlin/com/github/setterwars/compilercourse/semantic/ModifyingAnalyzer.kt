package com.github.setterwars.compilercourse.semantic


import com.github.setterwars.compilercourse.parser.nodes.*

class ModifyingAnalyzer() {
//
//    fun modifyAnalyze(program: Program): Program = modifyAnalyzeProgram(program)
//
//    fun modifyAnalyzeProgram(node: Program): Program {
//        val declarations = node.declarations.map { modifyAnalyzeDeclaration(it) }
//        return Program(declarations)
//    }
//
//    fun modifyAnalyzeBody(node: Body): Body {
//        val bodyElements = node.bodyElements.map { modifyAnalyzeBodyElement(it) }
//        return Body(bodyElements)
//    }
//
//    fun modifyAnalyzeBodyElement(node: BodyElement): BodyElement = when (node) {
//        is Statement -> modifyAnalyzeStatement(node)
//        is SimpleDeclaration -> modifyAnalyzeSimpleDeclaration(node)
//    }
//
//    fun modifyAnalyzeDeclaration(node: Declaration): Declaration = when (node) {
//        is RoutineDeclaration -> modifyAnalyzeRoutineDeclaration(node)
//        is SimpleDeclaration -> modifyAnalyzeSimpleDeclaration(node)
//    }
//
//    fun modifyAnalyzeSimpleDeclaration(node: SimpleDeclaration): SimpleDeclaration = when (node) {
//        is VariableDeclaration -> modifyAnalyzeVariableDeclaration(node)
//        is TypeDeclaration -> modifyAnalyzeTypeDeclaration(node)
//    }
//
//    fun modifyAnalyzeVariableDeclaration(node: VariableDeclaration): VariableDeclaration = when (node) {
//        is VariableDeclarationWithType -> modifyAnalyzeVariableDeclarationWithType(node)
//        is VariableDeclarationNoType -> modifyAnalyzeVariableDeclarationNoType(node)
//    }
//
//    fun modifyAnalyzeVariableDeclarationWithType(node: VariableDeclarationWithType): VariableDeclarationWithType {
//        val identifier = modifyAnalyzeIdentifier(node.identifier)
//        val type = modifyAnalyzeType(node.type)
//        val initialValue = node.initialValue?.let { modifyAnalyzeExpression(it) }
//        return VariableDeclarationWithType(identifier, type, initialValue)
//    }
//
//    fun modifyAnalyzeVariableDeclarationNoType(node: VariableDeclarationNoType): VariableDeclarationNoType {
//        val identifier = modifyAnalyzeIdentifier(node.identifier)
//        val initialValue = modifyAnalyzeExpression(node.initialValue)
//        return VariableDeclarationNoType(identifier, initialValue)
//    }
//
//    fun modifyAnalyzeTypeDeclaration(node: TypeDeclaration): TypeDeclaration {
//        val identifier = modifyAnalyzeIdentifier(node.identifier)
//        val type = modifyAnalyzeType(node.type)
//        return TypeDeclaration(identifier, type)
//    }
//
//    fun modifyAnalyzeRoutineDeclaration(node: RoutineDeclaration): RoutineDeclaration {
//        val header = modifyAnalyzeRoutineHeader(node.header)
//        val body = node.body?.let { modifyAnalyzeRoutineBody(it) }
//        return RoutineDeclaration(header, body)
//    }
//
//    fun modifyAnalyzeRoutineHeader(node: RoutineHeader): RoutineHeader {
//        val name = modifyAnalyzeIdentifier(node.name)
//        val parameters = modifyAnalyzeParameters(node.parameters)
//        val returnType = node.returnType?.let { modifyAnalyzeType(it) }
//        return RoutineHeader(name, parameters, returnType)
//    }
//
//    fun modifyAnalyzeRoutineBody(node: RoutineBody): RoutineBody = when (node) {
//        is FullRoutineBody -> modifyAnalyzeFullRoutineBody(node)
//        is SingleExpressionBody -> modifyAnalyzeSingleExpressionBody(node)
//    }
//
//    fun modifyAnalyzeFullRoutineBody(node: FullRoutineBody): FullRoutineBody {
//        val body = modifyAnalyzeBody(node.body)
//        return FullRoutineBody(body)
//    }
//
//    fun modifyAnalyzeSingleExpressionBody(node: SingleExpressionBody): SingleExpressionBody {
//        val expr = modifyAnalyzeExpression(node.expression)
//        return SingleExpressionBody(expr)
//    }
//
//    fun modifyAnalyzeParameters(node: Parameters): Parameters {
//        val params = node.parameters.map { modifyAnalyzeParameterDeclaration(it) }
//        return Parameters(params)
//    }
//
//    fun modifyAnalyzeParameterDeclaration(node: ParameterDeclaration): ParameterDeclaration {
//        val name = modifyAnalyzeIdentifier(node.name)
//        val type = modifyAnalyzeType(node.type)
//        return ParameterDeclaration(name, type)
//    }
//
//    fun modifyAnalyzeStatement(node: Statement): Statement = when (node) {
//        is Assignment -> modifyAnalyzeAssignment(node)
//        is RoutineCall -> modifyAnalyzeRoutineCall(node)
//        is WhileLoop -> modifyAnalyzeWhileLoop(node)
//        is ForLoop -> modifyAnalyzeForLoop(node)
//        is IfStatement -> modifyAnalyzeIfStatement(node)
//        is PrintStatement -> modifyAnalyzePrintStatement(node)
//        else -> node
//    }
//
//    fun modifyAnalyzeAssignment(node: Assignment): Assignment {
//        val lhs = modifyAnalyzeModifiablePrimary(node.modifiablePrimary)
//        val rhs = modifyAnalyzeExpression(node.expression)
//        return Assignment(lhs, rhs)
//    }
//
//    fun modifyAnalyzeRoutineCall(node: RoutineCall): RoutineCall {
//        val name = modifyAnalyzeIdentifier(node.routineName)
//        val args = node.arguments.map { modifyAnalyzeRoutineCallArgument(it) }
//        return RoutineCall(name, args)
//    }
//
//    fun modifyAnalyzeRoutineCallArgument(node: RoutineCallArgument): RoutineCallArgument {
//        val expr = modifyAnalyzeExpression(node.expression)
//        return RoutineCallArgument(expr)
//    }
//
//    fun modifyAnalyzeWhileLoop(node: WhileLoop): WhileLoop {
//        val condition = modifyAnalyzeExpression(node.condition)
//        val body = modifyAnalyzeBody(node.body)
//        return WhileLoop(condition, body)
//    }
//
//    fun modifyAnalyzeForLoop(node: ForLoop): ForLoop {
//        val loopVariable = modifyAnalyzeIdentifier(node.loopVariable)
//        val range = modifyAnalyzeRange(node.range)
//        val reverse = node.reverse
//        val body = modifyAnalyzeBody(node.body)
//        return ForLoop(loopVariable, range, reverse, body)
//    }
//
//    fun modifyAnalyzeRange(node: Range): Range {
//        val begin = modifyAnalyzeExpression(node.begin)
//        val end = node.end?.let { modifyAnalyzeExpression(it) }
//        return Range(begin, end)
//    }
//
//    fun modifyAnalyzeIfStatement(node: IfStatement): Statement {
//        val nodeInfo = semanticInfoStore.getOrNull<IfStatementSemanticInfo>(node)
//        return if (nodeInfo?.compiledCondition == true) {
//            println("Remove `else` branch from `IfStatement` | node: $node")
//            IfStatement(
//                condition = CalculatedPrimitiveValue.asExpression(BooleanValue(true)),
//                thenBody = modifyAnalyzeBody(node.thenBody),
//                elseBody = null
//            )
//        } else if (nodeInfo?.compiledCondition == false && node.elseBody == null) {
//            println("Removing `IfStatement` | node: $node")
//            RemovedStatement(removedStatementClassname = node::class.simpleName)
//        } else if (nodeInfo?.compiledCondition == false && node.elseBody != null) {
//            println("Remove `then` branch from `IfStatement` | node: $node")
//            IfStatement(
//                condition = CalculatedPrimitiveValue.asExpression(BooleanValue(true)),
//                thenBody = modifyAnalyzeBody(node.elseBody),
//                elseBody = null
//            )
//        } else {
//            val condition = modifyAnalyzeExpression(node.condition)
//            val thenBody = modifyAnalyzeBody(node.thenBody)
//            val elseBody = node.elseBody?.let { modifyAnalyzeBody(it) }
//            IfStatement(condition, thenBody, elseBody)
//        }
//    }
//
//    fun modifyAnalyzePrintStatement(node: PrintStatement): PrintStatement {
//        val head = modifyAnalyzeExpression(node.expression)
//        val rest = node.rest.map { modifyAnalyzeExpression(it) }
//        return PrintStatement(head, rest)
//    }
//
//    fun modifyAnalyzeExpression(node: Expression): Expression {
//        val nodeInfo = semanticInfoStore.getOrNull<ExpressionSemanticInfo>(node)
//        return if (nodeInfo?.const != null) {
//            val newNode = CalculatedPrimitiveValue.asExpression(nodeInfo.const)
//            println("Replacing `Expression` node with calculated compile-time value: ${nodeInfo.const} | node: $node")
//            semanticInfoStore.setSemanticInfo(
//                newNode,
//                ExpressionSemanticInfo(type = nodeInfo.type, const = nodeInfo.const)
//            )
//            newNode
//        } else {
//            node
//        }
//    }
//
//    fun modifyAnalyzeModifiablePrimary(node: ModifiablePrimary): ModifiablePrimary {
//        val variable = modifyAnalyzeIdentifier(node.variable)
//        val accessors = node.accessors?.map { modifyAnalyzeAccessor(it) }
//        return ModifiablePrimary(variable, accessors)
//    }
//
//    fun modifyAnalyzeAccessor(node: Accessor): Accessor = when (node) {
//        is FieldAccessor -> modifyAnalyzeFieldAccessor(node)
//        is ArrayAccessor -> modifyAnalyzeArrayAccessor(node)
//        else -> node
//    }
//
//    fun modifyAnalyzeFieldAccessor(node: FieldAccessor): FieldAccessor {
//        val id = modifyAnalyzeIdentifier(node.identifier)
//        return FieldAccessor(id)
//    }
//
//    fun modifyAnalyzeArrayAccessor(node: ArrayAccessor): ArrayAccessor {
//        val expr = modifyAnalyzeExpression(node.expression)
//        return ArrayAccessor(expr)
//    }
//
//    fun modifyAnalyzeIntegerLiteral(node: IntegerLiteral): IntegerLiteral = node
//    fun modifyAnalyzeRealLiteral(node: RealLiteral): RealLiteral = node
//
//    fun modifyAnalyzeUnaryOperator(node: UnaryOperator): UnaryOperator = when (node) {
//        is UnarySign -> modifyAnalyzeUnarySign(node)
//        is UnaryNot -> modifyAnalyzeUnaryNot(node)
//        else -> node
//    }
//
//    fun modifyAnalyzeUnaryRealOperator(node: UnaryRealOperator): UnaryRealOperator = when (node) {
//        is UnarySign -> modifyAnalyzeUnarySign(node)
//        else -> node
//    }
//
//    fun modifyAnalyzeUnarySign(node: UnarySign): UnarySign = node
//    fun modifyAnalyzeUnaryNot(mnode: UnaryNot): UnaryNot = UnaryNot
//
//    fun modifyAnalyzeIdentifier(node: Identifier): Identifier = node
//
//    fun modifyAnalyzeType(node: Type): Type = when (node) {
//        is PrimitiveType -> modifyAnalyzePrimitiveType(node)
//        is DeclaredType -> modifyAnalyzeDeclaredType(node)
//        is UserType -> modifyAnalyzeUserType(node)
//    }
//
//    fun modifyAnalyzePrimitiveType(node: PrimitiveType): PrimitiveType = node
//
//    fun modifyAnalyzeDeclaredType(node: DeclaredType): DeclaredType {
//        val id = modifyAnalyzeIdentifier(node.identifier)
//        return DeclaredType(id)
//    }
//
//    fun modifyAnalyzeUserType(node: UserType): UserType = when (node) {
//        is ArrayType -> modifyAnalyzeArrayType(node)
//        is RecordType -> modifyAnalyzeRecordType(node)
//    }
//
//    fun modifyAnalyzeArrayType(node: ArrayType): ArrayType {
//        val expr = node.expressionInBrackets?.let { modifyAnalyzeExpression(it) }
//        val type = modifyAnalyzeType(node.type)
//        return ArrayType(expr, type)
//    }
//
//    fun modifyAnalyzeRecordType(node: RecordType): RecordType {
//        val declarations = node.declarations.map { modifyAnalyzeVariableDeclaration(it) }
//        return RecordType(declarations)
//    }
}
