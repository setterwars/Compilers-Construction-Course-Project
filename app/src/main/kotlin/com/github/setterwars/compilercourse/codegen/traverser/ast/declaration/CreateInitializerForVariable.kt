package com.github.setterwars.compilercourse.codegen.traverser.ast.declaration

import com.github.setterwars.compilercourse.codegen.bytecode.ir.Block
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Br
import com.github.setterwars.compilercourse.codegen.bytecode.ir.BrIf
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Drop
import com.github.setterwars.compilercourse.codegen.bytecode.ir.GlobalGet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32BinOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Binary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Compare
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Const
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32RelOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Store
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32Unary
import com.github.setterwars.compilercourse.codegen.bytecode.ir.I32UnaryOp
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Instr
import com.github.setterwars.compilercourse.codegen.bytecode.ir.LocalSet
import com.github.setterwars.compilercourse.codegen.bytecode.ir.Loop
import com.github.setterwars.compilercourse.codegen.traverser.cell.CellValueType
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryArray
import com.github.setterwars.compilercourse.codegen.traverser.cell.InMemoryRecord
import com.github.setterwars.compilercourse.codegen.traverser.cell.MemoryReferencable
import com.github.setterwars.compilercourse.codegen.traverser.cell.Variable
import com.github.setterwars.compilercourse.codegen.traverser.cell.VariableType
import com.github.setterwars.compilercourse.codegen.traverser.cell.load
import com.github.setterwars.compilercourse.codegen.traverser.cell.store
import com.github.setterwars.compilercourse.codegen.traverser.cell.toWasmValue
import com.github.setterwars.compilercourse.codegen.traverser.common.MemoryManager
import com.github.setterwars.compilercourse.codegen.traverser.common.WasmContext
import com.github.setterwars.compilercourse.codegen.utils.randomString64

// Create instructions, that will allocate
// memory for memoryReferencable (and probably fill it with some value)
// Result = allocated address on the stack
fun WasmContext.createInitializerForMemoryReferencable(
    memoryReferencable: MemoryReferencable
): List<Instr> = buildList {
    val allocatedAddressHolderVariableName = "#allocatedAddressHolderVariableName${randomString64()}"
    declarationManager.declareLocalVariable(allocatedAddressHolderVariableName, CellValueType.I32)
    val allocatedAddressHolder = declarationManager.resolveVariable(allocatedAddressHolderVariableName)
    allocatedAddressHolder.store {
        MemoryManager.allocateBytes(memoryReferencable.inMemoryBytesSize)
    }

    // Nothing on stack so far
    when (memoryReferencable) {
        is InMemoryRecord -> {
            allocatedAddressHolder.load()
            memoryReferencable.initializer?.let { addAll(it) }

            var recordOffset = 0
            for (field in memoryReferencable.fields) {
                if (field.cellValueType is MemoryReferencable) {
                    allocatedAddressHolder.load()
                    addAll(createInitializerForMemoryReferencable(field.cellValueType))
                    add(I32Store())
                }
                recordOffset += field.cellValueType.toWasmValue().bytes
                allocatedAddressHolder.load()
                add(I32Const(recordOffset))
                add(I32Binary(I32BinOp.Add))
            }
            add(Drop)
            addAll(allocatedAddressHolder.load())
        }

        is InMemoryArray -> {
            if (memoryReferencable.cellValueType !is MemoryReferencable) {
                addAll(allocatedAddressHolder.load())
                return@buildList
            }

            val fillingIndexHolderVariableName = "#fillingIndexHolderVariableName${randomString64()}"
            declarationManager.declareLocalVariable(fillingIndexHolderVariableName, CellValueType.I32)
            val fillingIndexHolder = declarationManager.resolveVariable(fillingIndexHolderVariableName)
            fillingIndexHolder.store {
                listOf(I32Const(0))
            }
            fillingIndexHolder.load()

            add(
                Loop(
                    null,
                    instructions = buildList {
                        add(
                            Loop(
                                null,
                                instructions = buildList {
                                    addAll(fillingIndexHolder.load())
                                    add(I32Compare(I32RelOp.LtS))
                                    add(I32Unary(I32UnaryOp.EQZ))
                                    add(BrIf(1))

                                    addAll(allocatedAddressHolder.load())
                                    addAll(fillingIndexHolder.load())
                                    add(I32Const(memoryReferencable.cellValueType.toWasmValue().bytes))
                                    add(I32Binary(I32BinOp.Mul))
                                    add(I32Binary(I32BinOp.Add))

                                    createInitializerForMemoryReferencable(memoryReferencable.cellValueType)
                                    add(I32Store())

                                    addAll(fillingIndexHolder.load())
                                    add(I32Const(1))
                                    add(I32Binary(I32BinOp.Add))
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
                        add(Drop)
                        addAll(allocatedAddressHolder.load())
                    }
                )
            )

        }
    }
}