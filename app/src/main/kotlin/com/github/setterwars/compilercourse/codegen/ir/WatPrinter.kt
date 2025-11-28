package com.github.setterwars.compilercourse.codegen.ir


object WatPrinter {

    fun printModule(module: WasmModule): String {
        val sb = StringBuilder()

        // First, compute function indices for things like (start N)
        val funcIndexMap = mutableMapOf<WasmFunc, Int>()
        var funcCount = 0
        for (def in module.definitions) {
            if (def is WasmFunc) {
                funcIndexMap[def] = funcCount++
            }
        }

        sb.appendLine("(module")

        var indent = 1
        for (def in module.definitions) {
            printDefinition(sb, def, indent)
        }

        // Emit start function(s) if any
        val startFuncs = funcIndexMap.keys.filter { it.isStart }
        for (f in startFuncs) {
            val idx = funcIndexMap[f]!!
            sb.appendIndent(indent).appendLine("(start $idx)")
        }

        sb.appendLine(")")
        return sb.toString()
    }

    private fun printDefinition(sb: StringBuilder, def: WasmDefinition, indent: Int) {
        when (def) {
            is WasmFunc -> printFunc(sb, def, indent)
            is WasmI32Global -> printI32Global(sb, def, indent)
            is WasmF64Global -> printF64Global(sb, def, indent)
            is WasmMemory -> printMemory(sb, def, indent)
            is WasmExport -> printExport(sb, def, indent)
            else -> TODO()
            // If you add more WasmDefinition kinds later, handle them here.
        }
    }

    // ---- Functions ----

    private fun printFunc(sb: StringBuilder, func: WasmFunc, indent: Int) {
        sb.appendIndent(indent)
        sb.append("(func")

        // Optional name
        func.name?.let { sb.append(" \$").append(it) }

        // Params
        if (func.type.params.isNotEmpty()) {
            sb.append(" (param")
            for (p in func.type.params) {
                sb.append(' ').append(p.toWat())
            }
            sb.append(')')
        }

        // Results
        if (func.type.results.isNotEmpty()) {
            sb.append(" (result")
            for (r in func.type.results) {
                sb.append(' ').append(r.toWat())
            }
            sb.append(')')
        }

        sb.appendLine()

        // Locals (group all locals into one (local ...) for simplicity)
        if (func.locals.isNotEmpty()) {
            sb.appendIndent(indent + 1)
            sb.append("(local")
            for (local in func.locals) {
                sb.append(' ').append(local.toWat())
            }
            sb.appendLine(")")
        }

        // Body
        for (instr in func.body) {
            printInstr(sb, instr, indent + 1)
        }

        sb.appendIndent(indent).appendLine(")")
    }

    // ---- Globals ----

    private fun printI32Global(sb: StringBuilder, g: WasmI32Global, indent: Int) {
        sb.appendIndent(indent)
        if (g.mutable) {
            sb.append("(global (mut i32) (i32.const ").append(g.initValue).appendLine("))")
        } else {
            sb.append("(global i32 (i32.const ").append(g.initValue).appendLine("))")
        }
    }

    private fun printF64Global(sb: StringBuilder, g: WasmF64Global, indent: Int) {
        sb.appendIndent(indent)
        if (g.mutable) {
            sb.append("(global (mut f64) (f64.const ").append(g.initValue).appendLine("))")
        } else {
            sb.append("(global f64 (f64.const ").append(g.initValue).appendLine("))")
        }
    }

    // ---- Memory ----

    private fun printMemory(sb: StringBuilder, mem: WasmMemory, indent: Int) {
        sb.appendIndent(indent)
        if (mem.maxPages != null) {
            sb.append("(memory ").append(mem.minPages).append(' ').append(mem.maxPages).appendLine(")")
        } else {
            sb.append("(memory ").append(mem.minPages).appendLine(")")
        }
    }

    // ---- Exports ----

    private fun printExport(sb: StringBuilder, e: WasmExport, indent: Int) {
        sb.appendIndent(indent)
        sb.append("(export \"").append(e.name).append("\" ")
        when (e.kind) {
            ExportKind.Func -> sb.append("(func ").append(e.index).append(')')
            ExportKind.Memory -> sb.append("(memory ").append(e.index).append(')')
        }
        sb.appendLine(")")
    }

    // ---- Instructions ----

    private fun printInstr(sb: StringBuilder, instr: Instr, indent: Int) {
        when (instr) {
            is Unreachable -> sb.appendIndent(indent).appendLine("unreachable")
            is Nop -> sb.appendIndent(indent).appendLine("nop")

            is Block -> printBlock(sb, instr, indent)
            is Loop -> printLoop(sb, instr, indent)
            is If -> printIf(sb, instr, indent)
            is Br -> sb.appendIndent(indent).append("br ").append(instr.depth).appendLine()
            is BrIf -> sb.appendIndent(indent).append("br_if ").append(instr.depth).appendLine()
            Return -> sb.appendIndent(indent).appendLine("return")
            Drop -> sb.appendIndent(indent).appendLine("drop")

            is Call -> sb.appendIndent(indent).append("call ").append(instr.funcIndex).appendLine()

            is LocalGet -> sb.appendIndent(indent).append("local.get ").append(instr.index).appendLine()
            is LocalSet -> sb.appendIndent(indent).append("local.set ").append(instr.index).appendLine()
            is LocalTee -> sb.appendIndent(indent).append("local.tee ").append(instr.index).appendLine()

            is I32Const -> sb.appendIndent(indent).append("i32.const ").append(instr.value).appendLine()
            is F64Const -> sb.appendIndent(indent).append("f64.const ").append(instr.value).appendLine()

            is I32Binary -> sb.appendIndent(indent).appendLine(instr.op.toWat())
            is I32Unary -> sb.appendIndent(indent).appendLine(instr.op.toWat())
            is F64Binary -> sb.appendIndent(indent).appendLine(instr.op.toWat())
            is I32Compare -> sb.appendIndent(indent).appendLine(instr.op.toWat())
            is F64Compare -> sb.appendIndent(indent).appendLine(instr.op.toWat())

            is GlobalGet -> sb.appendIndent(indent).append("global.get ").append(instr.index).appendLine()
            is GlobalSet -> sb.appendIndent(indent).append("global.set ").append(instr.index).appendLine()

            is I32Load -> printLoadStore(sb, indent, "i32.load", instr.align, instr.offset)
            is I32Store -> printLoadStore(sb, indent, "i32.store", instr.align, instr.offset)
            is F64Load -> printLoadStore(sb, indent, "f64.load", instr.align, instr.offset)
            is F64Store -> printLoadStore(sb, indent, "f64.store", instr.align, instr.offset)
        }
    }

    private fun printBlock(sb: StringBuilder, block: Block, indent: Int) {
        sb.appendIndent(indent)
        sb.append("(block")
        block.resultType?.let {
            sb.append(" (result ").append(it.toWat()).append(')')
        }
        sb.appendLine()

        for (instr in block.instructions) {
            printInstr(sb, instr, indent + 1)
        }

        sb.appendIndent(indent).appendLine(")")
    }

    private fun printLoop(sb: StringBuilder, loop: Loop, indent: Int) {
        sb.appendIndent(indent)
        sb.append("(loop")
        loop.resultType?.let {
            sb.append(" (result ").append(it.toWat()).append(')')
        }
        sb.appendLine()

        for (instr in loop.instructions) {
            printInstr(sb, instr, indent + 1)
        }

        sb.appendIndent(indent).appendLine(")")
    }

    private fun printIf(sb: StringBuilder, ifInstr: If, indent: Int) {
        sb.appendIndent(indent)
        sb.append("(if")
        ifInstr.resultType?.let {
            sb.append(" (result ").append(it.toWat()).append(')')
        }
        sb.appendLine()

        // then
        sb.appendIndent(indent + 1).appendLine("(then")
        for (instr in ifInstr.thenInstrs) {
            printInstr(sb, instr, indent + 2)
        }
        sb.appendIndent(indent + 1).appendLine(")")

        // else
        if (ifInstr.elseInstrs.isNotEmpty()) {
            sb.appendIndent(indent + 1).appendLine("(else")
            for (instr in ifInstr.elseInstrs) {
                printInstr(sb, instr, indent + 2)
            }
            sb.appendIndent(indent + 1).appendLine(")")
        }

        sb.appendIndent(indent).appendLine(")")
    }

    private fun printLoadStore(
        sb: StringBuilder,
        indent: Int,
        op: String,
        alignPower: Int,
        offset: Int
    ) {
        val alignBytes = 1 shl alignPower
        sb.appendIndent(indent)
        sb.append(op)
        if (offset != 0 || alignPower != 0) {
            if (offset != 0) sb.append(" offset=").append(offset)
            if (alignPower != 0) sb.append(" align=").append(alignBytes)
        }
        sb.appendLine()
    }

    // ---- Mapping helpers ----

    private fun WasmValue.toWat(): String = when (this) {
        WasmValue.I32 -> "i32"
        WasmValue.F64 -> "f64"
    }

    private fun I32BinOp.toWat(): String = when (this) {
        I32BinOp.Add -> "i32.add"
        I32BinOp.Sub -> "i32.sub"
        I32BinOp.Mul -> "i32.mul"
        I32BinOp.DivS -> "i32.div_s"
        I32BinOp.DivU -> "i32.div_u"
        I32BinOp.RemS -> "i32.rem_s"
        I32BinOp.RemU -> "i32.rem_u"
        I32BinOp.And -> "i32.and"
        I32BinOp.Or -> "i32.or"
        I32BinOp.Xor -> "i32.xor"
    }

    private fun I32UnaryOp.toWat(): String = when (this) {
        I32UnaryOp.EQZ -> "i32.eqz"
    }

    private fun F64BinOp.toWat(): String = when (this) {
        F64BinOp.Add -> "f64.add"
        F64BinOp.Sub -> "f64.sub"
        F64BinOp.Mul -> "f64.mul"
        F64BinOp.Div -> "f64.div"
    }

    private fun I32RelOp.toWat(): String = when (this) {
        I32RelOp.Eq -> "i32.eq"
        I32RelOp.Ne -> "i32.ne"
        I32RelOp.LtS -> "i32.lt_s"
        I32RelOp.LtU -> "i32.lt_u"
        I32RelOp.GtS -> "i32.gt_s"
        I32RelOp.GtU -> "i32.gt_u"
        I32RelOp.LeS -> "i32.le_s"
        I32RelOp.LeU -> "i32.le_u"
        I32RelOp.GeS -> "i32.ge_s"
        I32RelOp.GeU -> "i32.ge_u"
    }

    private fun F64RelOp.toWat(): String = when (this) {
        F64RelOp.Eq -> "f64.eq"
        F64RelOp.Ne -> "f64.ne"
        F64RelOp.Lt -> "f64.lt"
        F64RelOp.Gt -> "f64.gt"
        F64RelOp.Le -> "f64.le"
        F64RelOp.Ge -> "f64.ge"
    }

    // ---- Indent helper ----

    private fun StringBuilder.appendIndent(indent: Int): StringBuilder {
        repeat(indent) { append("  ") } // 2 spaces per indent level
        return this
    }
}
