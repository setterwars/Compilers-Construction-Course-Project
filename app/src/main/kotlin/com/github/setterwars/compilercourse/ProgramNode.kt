package com.github.setterwars.compilercourse

sealed interface ASTNode
sealed interface TypeNode: ASTNode // node for types
sealed interface ExprNode: ASTNode // node for expressions
sealed interface StmtNode : ASTNode // node for statements
sealed interface SelectorNode: ASTNode // node for selectors

data class ProgramNode( // class for call, need list of ASTNodes
    val declarations: List<ASTNode>
) : ASTNode

// Nodes for declarations
data class VarDeclNode(
    val name: String,
    val type: TypeNode?,
    val init: ExprNode?
) : ASTNode

data class NewLineNode(val name: String = "newLine") : ASTNode

data class TypeDeclNode(
    val name: String,
    val baseType: TypeNode
) : ASTNode

data class RoutineDeclNode(
    val name: String,
    val params: List<ParamNode>,
    val returnType: TypeNode?,
    val body: BodyNode?
) : ASTNode

// Node for parametrs
data class ParamNode(
    val name: String,
    val type: TypeNode
) : ASTNode

// Nodes for types
data class PrimitiveTypeNode(val kind: Kind) : TypeNode {
    enum class Kind {INTEGER, REAL, BOOLEAN}
}


data class ArrayTypeNode(
    val size: ExprNode?,
    val elemType: TypeNode,
)  : TypeNode

data class RecordTypeNode(
    val fields: List<VarDeclNode>
) : TypeNode

data class NamedTypeNode(
    val name: String
) : TypeNode

// Nodes for statements

data class AssignNode(
    val target: ModifiablePrimaryNode,
    val value: ExprNode,
) : StmtNode

data class CallNode(
    val name: String,
    val args: List<ExprNode>,
) : StmtNode

data class WhileNode(
    val condition: ExprNode,
    val body: BodyNode
) : StmtNode

data class ForNode(
    val variable: String,
    val range: RangeNode,
    val reverse: Boolean,
    val body: BodyNode
) : StmtNode

data class IfNode(
    val condition: ExprNode,
    val thenBody: BodyNode,
    val elseBody: BodyNode?
) : StmtNode

data class PrintNode(
    val args: List<ExprNode>
) : StmtNode

data class BodyNode(
    val items: List<ASTNode>
) : ASTNode

data class ExprStmtNode(val expr: ExprNode) : StmtNode

// Nodes for expressions
data class BinaryOpNode(
    val left: ExprNode,
    val op: String,
    val right: ExprNode
) : ExprNode

data class UnaryOpNode(
    val op: String,
    val expr: ExprNode
) : ExprNode

data class LiteralNode(
    val value: Any
) : ExprNode

data class VarRefNode(
    val name: String
) : ExprNode

data class CallExprNode(
    val name: String,
    val args: List<ExprNode>
) : ExprNode

data class ModifiablePrimaryNode(
    val base: String,
    val selectors: List<SelectorNode>
) : ExprNode

data class FieldAccessNode(
    val field: String
) : SelectorNode

data class IndexAccessNode(
    val index: ExprNode
) : SelectorNode

data class RangeNode(
    val start: ExprNode?,
    val end: ExprNode?
) : ASTNode

data class EOFNode(
    val name: String = "EOF"
) : ASTNode

data class ReturnNode(
    val expr: ExprNode? = null
) : ASTNode


// Test function just for pretty printing of program tree
fun prettyPrint(node: ASTNode, indent: Int = 0) {
    val pad = "  ".repeat(indent)

    when (node) {
        is ProgramNode -> {
            println("${pad}Program")
            node.declarations.forEach { prettyPrint(it, indent + 1) }
        }

        is VarDeclNode -> {
            val typeName = when (val t = node.type) {
                is PrimitiveTypeNode -> t.kind.toString().lowercase()
                is ArrayTypeNode -> "array"
                is RecordTypeNode -> "record"
                is NamedTypeNode -> t.name
                null -> "inferred"
            }
            println("${pad}VarDecl ${node.name} : $typeName")
            node.init?.let { prettyPrint(it, indent + 1) }
        }

        is TypeDeclNode -> {
            println("${pad}TypeDecl ${node.name}")
            prettyPrint(node.baseType, indent + 1)
        }

        is RoutineDeclNode -> {
            println("${pad}RoutineDecl ${node.name}")
            node.params.forEach { prettyPrint(it, indent + 1) }
            node.returnType?.let { prettyPrint(it, indent + 1) }
            node.body?.let { prettyPrint(it, indent + 1) }
        }

        is ParamNode -> println("${pad}Param ${node.name} : ${(node.type as? PrimitiveTypeNode)?.kind}")

        is PrimitiveTypeNode -> println("${pad}PrimitiveType ${node.kind}")

        is ArrayTypeNode -> {
            println("${pad}ArrayType")
            node.size?.let { prettyPrint(it, indent + 1) }
            prettyPrint(node.elemType, indent + 1)
        }

        is RecordTypeNode -> {
            println("${pad}RecordType")
            node.fields.forEach { prettyPrint(it, indent + 1) }
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

        is BodyNode -> {
            println("${pad}Body")
            node.items.forEach { prettyPrint(it, indent + 1) }
        }

        is BinaryOpNode -> {
            println("${pad}BinaryOp ${node.op}")
            prettyPrint(node.left, indent + 1)
            prettyPrint(node.right, indent + 1)
        }

        is UnaryOpNode -> {
            println("${pad}UnaryOp ${node.op}")
            prettyPrint(node.expr, indent + 1)
        }

        is LiteralNode -> println("${pad}Literal ${node.value}")

        is VarRefNode -> println("${pad}VarRef ${node.name}")

        is CallExprNode -> {
            println("${pad}CallExpr ${node.name}")
            node.args.forEach { prettyPrint(it, indent + 1) }
        }

        is ModifiablePrimaryNode -> {
            println("${pad}Var ${node.base}")
            node.selectors.forEach { prettyPrint(it, indent + 1) }
        }

        is FieldAccessNode -> println("${pad}.${node.field}")

        is IndexAccessNode -> {
            println("${pad}[index]")
            prettyPrint(node.index, indent + 1)
        }

        is RangeNode -> {
            println("${pad}Range")
            node.start?.let { prettyPrint(it, indent + 1) }
            node.end?.let { prettyPrint(it, indent + 1) }
        }

        else -> println("${pad}${node}")
    }
}
