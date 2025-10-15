package com.github.setterwars.compilercourse

/**
 * AST (ADT) definitions for the Imperative Language (basic nodes).
 * This covers nodes specified in Project I grammar:
 * - Declarations: VarDecl, TypeDecl, RoutineDecl
 * - Types: Primitive, Array, Record, Named
 * - Statements: Assign, Call, While, For, If, Print, Return, ExprStmt
 * - Expressions: Binary, Unary, Literal, VarRef, CallExpr, ModifiablePrimary
 * - Selectors: FieldAccess, IndexAccess
 *
 * Keep this file lightweight and extendable.
 */

sealed interface ASTNode

// categorize nodes for clarity
sealed interface TypeNode : ASTNode
sealed interface ExprNode : ASTNode
sealed interface StmtNode : ASTNode
sealed interface SelectorNode : ASTNode

// Top-level program: sequence of declarations
data class ProgramNode(val declarations: List<ASTNode>) : ASTNode

// --- Declarations ---
data class VarDeclNode(
    val name: String,
    val type: TypeNode?,    // null -> inferred from initializer
    val init: ExprNode?     // optional initializer
) : ASTNode

data class TypeDeclNode(
    val name: String,
    val baseType: TypeNode
) : ASTNode

data class RoutineDeclNode(
    val name: String,
    val params: List<ParamNode>,
    val returnType: TypeNode?,
    val body: BodyNode?     // null -> forward declaration
) : ASTNode

data class ParamNode(val name: String, val type: TypeNode) : ASTNode

data class BodyNode(val items: List<ASTNode>) : ASTNode

// --- Types ---
data class PrimitiveTypeNode(val kind: Kind) : TypeNode {
    enum class Kind { INTEGER, REAL, BOOLEAN }
}

data class ArrayTypeNode(val size: ExprNode?, val elemType: TypeNode) : TypeNode
data class RecordTypeNode(val fields: List<VarDeclNode>) : TypeNode
data class NamedTypeNode(val name: String) : TypeNode

// --- Statements ---
data class AssignNode(val target: ModifiablePrimaryNode, val value: ExprNode) : StmtNode
data class CallNode(val name: String, val args: List<ExprNode>) : StmtNode
data class WhileNode(val condition: ExprNode, val body: BodyNode) : StmtNode
data class ForNode(val variable: String, val range: RangeNode, val reverse: Boolean, val body: BodyNode) : StmtNode
data class IfNode(val condition: ExprNode, val thenBody: BodyNode, val elseBody: BodyNode?) : StmtNode
data class PrintNode(val args: List<ExprNode>) : StmtNode
data class ReturnNode(val expr: ExprNode? = null) : StmtNode
data class ExprStmtNode(val expr: ExprNode) : StmtNode

// Range node used by for-loops
data class RangeNode(val start: ExprNode?, val end: ExprNode?) : ASTNode

// --- Expression nodes ---
data class BinaryOpNode(val left: ExprNode, val op: String, val right: ExprNode) : ExprNode
data class UnaryOpNode(val op: String, val expr: ExprNode) : ExprNode
data class LiteralNode(val value: Any) : ExprNode
data class VarRefNode(val name: String) : ExprNode
data class CallExprNode(val name: String, val args: List<ExprNode>) : ExprNode

// Modifiable primary is also an expression (can appear in rvalues too)
data class ModifiablePrimaryNode(val base: String, val selectors: List<SelectorNode>) : ExprNode

// Selectors
data class FieldAccessNode(val field: String) : SelectorNode
data class IndexAccessNode(val index: ExprNode) : SelectorNode

// EOF node (optional)
data class EOFNode(val name: String = "EOF") : ASTNode

// --- Pretty-print utility (optional) ---
fun prettyPrint(node: ASTNode, indent: Int = 0) {
    val pad = "  ".repeat(indent)
    when (node) {
        is ProgramNode -> {
            println("${pad}Program")
            node.declarations.forEach { prettyPrint(it, indent + 1) }
        }
        is VarDeclNode -> {
            val t = when (val ty = node.type) {
                is PrimitiveTypeNode -> ty.kind.name.lowercase()
                is ArrayTypeNode -> "array"
                is RecordTypeNode -> "record"
                is NamedTypeNode -> ty.name
                null -> "inferred"
            }
            println("${pad}VarDecl ${node.name} : $t")
            node.init?.let { prettyPrint(it, indent + 1) }
        }
        is TypeDeclNode -> {
            println("${pad}TypeDecl ${node.name}")
            prettyPrint(node.baseType, indent + 1)
        }
        is RoutineDeclNode -> {
            println("${pad}Routine ${node.name} params=${node.params.size} return=${node.returnType != null}")
            node.body?.let { prettyPrint(it, indent + 1) }
        }
        is BodyNode -> {
            println("${pad}Body")
            node.items.forEach { prettyPrint(it, indent + 1) }
        }
        is AssignNode -> {
            println("${pad}Assign")
            prettyPrint(node.target, indent + 1)
            prettyPrint(node.value, indent + 1)
        }
        is CallNode -> {
            println("${pad}Call ${node.name}")
            node.args.forEach { prettyPrint(it, indent + 1) }
        }
        is WhileNode -> {
            println("${pad}While")
            prettyPrint(node.condition, indent + 1)
            prettyPrint(node.body, indent + 1)
        }
        is ForNode -> {
            println("${pad}For ${node.variable} reverse=${node.reverse}")
            prettyPrint(node.range, indent + 1)
            prettyPrint(node.body, indent + 1)
        }
        is IfNode -> {
            println("${pad}If")
            prettyPrint(node.condition, indent + 1)
            println("${pad}Then")
            prettyPrint(node.thenBody, indent + 1)
            node.elseBody?.let {
                println("${pad}Else")
                prettyPrint(it, indent + 1)
            }
        }
        is PrintNode -> {
            println("${pad}Print")
            node.args.forEach { prettyPrint(it, indent + 1) }
        }
        is ReturnNode -> {
            println("${pad}Return")
            node.expr?.let { prettyPrint(it, indent + 1) }
        }
        is ExprStmtNode -> {
            println("${pad}ExprStmt")
            prettyPrint(node.expr, indent + 1)
        }
        is BinaryOpNode -> {
            println("${pad}BinaryOp '${node.op}'")
            prettyPrint(node.left, indent + 1)
            prettyPrint(node.right, indent + 1)
        }
        is UnaryOpNode -> {
            println("${pad}UnaryOp '${node.op}'")
            prettyPrint(node.expr, indent + 1)
        }
        is LiteralNode -> println("${pad}Literal ${node.value}")
        is VarRefNode -> println("${pad}VarRef ${node.name}")
        is CallExprNode -> {
            println("${pad}CallExpr ${node.name}")
            node.args.forEach { prettyPrint(it, indent + 1) }
        }
        is ModifiablePrimaryNode -> {
            println("${pad}Modifiable ${node.base}")
            node.selectors.forEach { prettyPrint(it, indent + 1) }
        }
        is FieldAccessNode -> println("${pad}. ${node.field}")
        is IndexAccessNode -> {
            println("${pad}[index]")
            prettyPrint(node.index, indent + 1)
        }
        is RangeNode -> {
            println("${pad}Range")
            node.start?.let { prettyPrint(it, indent + 1) }
            node.end?.let { prettyPrint(it, indent + 1) }
        }
        else -> println("${pad}Unknown node: $node")
    }
}
