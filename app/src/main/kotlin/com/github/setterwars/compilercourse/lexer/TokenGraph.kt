package com.github.setterwars.compilercourse.lexer

private interface Edge {
    fun canGo(c: Char): Boolean
}

private class Node(
    val transitions: MutableList<Pair<Edge, Node>> = mutableListOf(),
    val tokenType: TokenType? = null,
    val priority: Int? = null, // higher priority wins lower priority
)

private class GraphBuilder {
    private val nodes = mutableMapOf<Any, Node>()

    fun node(id: Any, tokenType: TokenType? = null, priority: Int? = null) {
        check(nodes[id] == null) { "Node $id is already defined" }
        nodes[id] = Node(tokenType = tokenType, priority = priority)
    }

    fun edge(from: Any, to: Any, canGo: (Char) -> Boolean) {
        val fromNode = nodes[from]
        checkNotNull(fromNode) { "'from' node $from is not defined" }
        val toNode = nodes[to]
        checkNotNull(toNode) { "'to' node $to is not defined" }
        fromNode.transitions.add(
            object : Edge {
                override fun canGo(c: Char): Boolean = canGo(c)
            } to toNode
        )
    }

    fun getNode(id: Any): Node {
        return nodes[id] ?: error("Node $id is not defined")
    }
}

private fun graph(rootNode: Any, builder: GraphBuilder.() -> Unit): Node {
    val graphBuilder = GraphBuilder()
    graphBuilder.builder()
    return graphBuilder.getNode(rootNode)
}

private val ROOT = graph(rootNode = "root") {
    // --- identifiers ---
    node(id= "root")
    node(id = "id", tokenType = TokenType.IDENTIFIER, priority = 0)
    edge("root", "id") { c -> c.isLetter() || c == '_' }
    edge("id", "id") { c -> c.isLetterOrDigit() || c == '_' }

    // --- integer & real literals ---
    node(id = "int", tokenType = TokenType.INT_LITERAL, priority = 0)
    node(id = "real_dot")
    node(id = "real_frac", tokenType = TokenType.REAL_LITERAL, priority = 0)
    edge("root", "int") { c -> c.isDigit() }
    edge("int", "int") { c -> c.isDigit() }
    edge("int", "real_dot") { c -> c == '.' }
    edge("real_dot", "real_frac") { c -> c.isDigit() }
    edge("real_frac", "real_frac") { c -> c.isDigit() }

    // --- punctuation ---
    node("colon", TokenType.COLON, 1)
    edge("root", "colon") { c -> c == ':' }

    node("lbracket", TokenType.LBRACKET, 1)
    edge("root", "lbracket") { c -> c == '[' }

    node("rbracket", TokenType.RBRACKET, 1)
    edge("root", "rbracket") { c -> c == ']' }

    node("assign", TokenType.ASSIGN, 1)
    edge("colon", "assign") { c -> c == '=' }

    node("comma", TokenType.COMMA, 1)
    edge("root", "comma") { c -> c == ',' }

    node("lparen", TokenType.LPAREN, 1)
    edge("root", "lparen") { c -> c == '(' }

    node("rparen", TokenType.RPAREN, 1)
    edge("root", "rparen") { c -> c == ')' }

    node("whitespace", TokenType.WHITESPACE, priority = 1)
    edge("root", "whitespace") { c -> c.isWhitespace() && c != '\n' }
    edge("whitespace", "whitespace") { c -> c.isWhitespace() && c != '\n'}

    node("dot1")
    node("range", TokenType.RANGE, 1)
    edge("root", "dot1") { c -> c == '.' }
    edge("dot1", "range") { c -> c == '.' }

    node("eq", TokenType.EQ, 1)
    edge("root", "eq") { c -> c == '=' }

    node("arrow", TokenType.ARROW, 1)
    edge("eq", "arrow") { c -> c == '>' }

    // relational operators
    node("lt", TokenType.LT, 1)
    edge("root", "lt") { c -> c == '<' }
    node("le", TokenType.LE, 2)
    edge("lt", "le") { c -> c == '=' }

    node("gt", TokenType.GT, 1)
    edge("root", "gt") { c -> c == '>' }
    node("ge", TokenType.GE, 2)
    edge("gt", "ge") { c -> c == '=' }


    node("slash")
    node("ne", TokenType.NE, 1)
    edge("root", "slash") { c -> c == '/' }
    edge("slash", "ne") { c -> c == '=' }

    // arithmetic operators
    node("plus", TokenType.PLUS, 1)
    edge("root", "plus") { c -> c == '+' }

    node("minus", TokenType.MINUS, 1)
    edge("root", "minus") { c -> c == '-' }

    node("star", TokenType.STAR, 1)
    edge("root", "star") { c -> c == '*' }

    node("slash_token", TokenType.SLASH, 1)
    edge("root", "slash_token") { c -> c == '/' }

    node("percent", TokenType.PERCENT, 1)
    edge("root", "percent") { c -> c == '%' }

    node("dot", TokenType.DOT, 1)
    edge("root", "dot") { c -> c == '.' }

    // --- keywords & boolean literals ---
    fun keyword(word: String, type: TokenType) {
        var prev = "root"
        for ((i, ch) in word.withIndex()) {
            val id = "$word$i"
            if (i == word.lastIndex) {
                node(id, type, 2) // higher priority than IDENTIFIER
            } else {
                node(id)
            }
            edge(prev, id) { c -> c == ch }
            prev = id
        }
    }

    keyword("false", TokenType.FALSE)
    keyword("true", TokenType.TRUE)
    keyword("and", TokenType.AND)
    keyword("or", TokenType.OR)
    keyword("xor", TokenType.XOR)
    keyword("var", TokenType.VAR)
    keyword("is", TokenType.IS)
    keyword("type", TokenType.TYPE)
    keyword("integer", TokenType.INTEGER)
    keyword("real", TokenType.REAL)
    keyword("boolean", TokenType.BOOLEAN)
    keyword("record", TokenType.RECORD)
    keyword("end", TokenType.END)
    keyword("array", TokenType.ARRAY)
    keyword("while", TokenType.WHILE)
    keyword("loop", TokenType.LOOP)
    keyword("for", TokenType.FOR)
    keyword("in", TokenType.IN)
    keyword("reverse", TokenType.REVERSE)
    keyword("if", TokenType.IF)
    keyword("then", TokenType.THEN)
    keyword("else", TokenType.ELSE)
    keyword("print", TokenType.PRINT)
    keyword("routine", TokenType.ROUTINE)
    keyword("not", TokenType.NOT)
    keyword("return", TokenType.RETURN)

    // --- layout ---
    node("newline", TokenType.NEW_LINE, 1)
    edge("root", "newline") { c -> c == '\n' }
}

class TokenGraph {
    private val currentStack = mutableListOf<Char>()
    private var currentNodes = setOf(ROOT)

    private fun reset() {
        currentStack.clear()
        currentNodes = setOf(ROOT)
    }

    private fun moveOnCharacter(c: Char): Set<Node> {
        val newNodes = mutableSetOf<Node>()
        for (node in currentNodes) {
            for (transition in node.transitions) {
                if (transition.first.canGo(c)) {
                    newNodes.add(transition.second)
                }
            }
        }
        return newNodes
    }

    // The current set of nodes contain at least one valid edge along character `c`
    fun canExpandOnCharacter(c: Char): Boolean {
        return moveOnCharacter(c).isNotEmpty()
    }

    // Find the node with non-null token type and max priority
    // Returns `null` is non of the current nodes are accepting
    fun determine(): Pair<TokenType, String>? {
        var maxPriority = -1
        var tokenType: TokenType? = null
        for (node in currentNodes) {
            if (node.tokenType != null && node.priority != null && node.priority > maxPriority) {
                tokenType = node.tokenType
                maxPriority = node.priority
            }
        }
        val lexeme = currentStack.joinToString("")
        return tokenType?.let { it to lexeme }
    }

    // Expand the current character set along the character c
    // If the expansion failed, then reset to the ROOT node
    fun feed(c: Char) {
        val newNodes = moveOnCharacter(c)
        if (newNodes.isEmpty()) {
            reset()
        } else {
            currentStack.add(c)
            currentNodes = newNodes
        }
    }
}
