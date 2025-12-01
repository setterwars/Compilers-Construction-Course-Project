package com.github.setterwars.compilercourse.codegen.traverser.ast.declaration

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Br
import com.github.setterwars.compilercourse.codegen.bytecode.ir.BrIf
import com.github.setterwars.compilercourse.codegen.bytecode.ir.F64Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Compare
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32RelOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Unary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32UnaryOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Loop
import com.github.setterwars.compilercourse.codegen.bytecode.ir.WasmValue
import com.github.setterwars.compilercourse.codegen.traverser.ast.expression.resolveExpression
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryArray
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryRecord
import com.github.setterwars.compilercourse.codegen.traverser.cell.MemoryReferencable
import com.github.setterwars.compilercourse.codegen.traverser.cell.adjustStackValue
import com.github.setterwars.compilercourse.codegen.traverser.cell.declareHelperVariable
import com.github.setterwars.compilercourse.codegen.traverser.cell.load
import com.github.setterwars.compilercourse.codegen.traverser.cell.store
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.MemoryManager
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext

// Create instructions, that will allocate
// memory for memoryReferencable (and probably fill it with some value)
// Result = allocated address on the stack
fun WasmContext.createInitializerForMemoryReferencable(
    memoryReferencable: MemoryReferencable
): List<Instr> = buildList {
    val allocatedAddressHolder = declareHelperVariable(
        "allocatedAddressHolderVariableName",
        CellValueType.I32
    ).let { (v, iss) -> addAll(iss); v }
    addAll(
        allocatedAddressHolder.store {
            MemoryManager.allocateBytes(memoryReferencable.inMemoryBytesSize)
        }
    )

    // Nothing on stack so far
    when (memoryReferencable) {
        is InMemoryRecord -> {
            var recordOffset = 0
            for (field in memoryReferencable.fields) {
                if (field.cellValueType is CellValueType.MemoryReference) {
                    addAll(allocatedAddressHolder.load())
                    addAll(createInitializerForMemoryReferencable(field.cellValueType.referencable))
                    add(I32Store())
                } else if (field.initialValue != null) {
                    addAll(allocatedAddressHolder.load())
                    val er = resolveExpression(field.initialValue)
                    addAll(er.instructions)
                    addAll(adjustStackValue(field.cellValueType, er.onStackValueType))
                    when (field.cellValueType.toWasmValue()) {
                        WasmValue.I32 -> add(I32Store())
                        WasmValue.F64 -> add(F64Store())
                    }
                    add(I32Store())
                }
                recordOffset += field.cellValueType.toWasmValue().bytes
                addAll(
                    allocatedAddressHolder.store { // TODO: fix error
                        buildList {
                            addAll(allocatedAddressHolder.load())
                            add(I32Const(recordOffset))
                            add(I32Binary(I32BinOp.Add))
                        }
                    }
                )
            }
            addAll(allocatedAddressHolder.load())
        }

        is InMemoryArray -> {
            if (memoryReferencable.cellValueType !is CellValueType.MemoryReference) {
                addAll(allocatedAddressHolder.load())
                return@buildList
            }

            val fillingIndexHolder = declareHelperVariable(
                "fillingIndexHolderName",
                CellValueType.I32
            ).let { (v, iss) -> addAll(iss); v }
            addAll(
                fillingIndexHolder.store {
                    listOf(I32Const(0))
                }
            )

            add(
                Loop(
                    null,
                    instructions = buildList {
                        addAll(fillingIndexHolder.load())
                        add(I32Const(memoryReferencable.size!!))
                        add(I32Compare(I32RelOp.LtS))
                        add(I32Unary(I32UnaryOp.EQZ))
                        add(BrIf(1))

                        addAll(allocatedAddressHolder.load())
                        addAll(fillingIndexHolder.load())
                        add(I32Const(memoryReferencable.cellValueType.toWasmValue().bytes))
                        add(I32Binary(I32BinOp.Mul))
                        add(I32Binary(I32BinOp.Add))

                        addAll(
                            createInitializerForMemoryReferencable(
                                memoryReferencable.cellValueType.referencable
                            )
                        )
                        add(I32Store())

                        addAll(fillingIndexHolder.store {
                            buildList {
                                addAll(fillingIndexHolder.load())
                                add(I32Const(1))
                                add(I32Binary(I32BinOp.Add))
                            }
                        })
                        add(Br(0))
                    }
                )
            )
            addAll(allocatedAddressHolder.load())
        }
    }
}