# Agents Guide: WebAssembly Codegen for Language I

This repository is a Kotlin compiler for a small imperative language (“Language I”).

- **Already implemented**: lexer, parser, semantic analysis.
- All codegen code must live in `codegen/` under package:
  `com.github.setterwars.compilercourse.codegen`.

Always read this file before changing anything.

---

## 1. Project Overview

### Existing pipeline

The main pipeline (see `Main.kt`) is:

1. Lexing → `Lexer`
2. Parsing → `Parser.parse()` returns `Program`
3. Semantic analysis → `SemanticAnalyzer.analyze(program)`
4. Modification pass → `ModifyingAnalyzer.modifyAnalyze(program)`
5. **(To be implemented)** code generation from the modified `Program` plus semantic info.

The AST is defined under `com.github.setterwars.compilercourse.parser.nodes`:

- `Program`, `Declaration`, `VariableDeclaration*`, `TypeDeclaration`
- Types: `PrimitiveType`, `ArrayType`, `RecordType`, `DeclaredType`
- Routines: `RoutineDeclaration`, `RoutineHeader`, `RoutineBody`, `Parameters`, `ParameterDeclaration`
- Bodies: `Body`, `BodyElement`
- Statements: `Assignment`, `RoutineCall`, `WhileLoop`, `ForLoop`, `IfStatement`, `PrintStatement`
- Expressions & primaries: `Expression`, `Relation`, `Simple`, `Factor`, `Summand`, `Primary`, `Unary*`, `ModifiablePrimary`, `BooleanLiteral`, `IntegerLiteral`, `RealLiteral`

The grammar is given in `Specs.md` (EBNF) and the PDF (“Project I”) contains the informal semantics.

---

## 2. Language Snapshot (what you need for codegen)

This is a compressed view so you don’t have to keep re-reading the specs.

### 2.1 Declarations and types

- A **program** is a sequence of:
    - Variable declarations
    - Type declarations
    - Routine declarations (top-level only; no nested routines)
- Variables:
    - `var x : Type [ is Expression ]`
    - `var x is Expression` (type inferred by semantics).
- Types:
    - Primitive types: `integer`, `real`, `boolean`
    - User types:
        - `record { VariableDeclaration } end`
        - `array [ [ Expression ] ] Type`
    - `type Name is Type` introduces a synonym (alias) type; the alias name (`DeclaredType`) can be used anywhere a type is expected.

**Semantics highlights:**

- All identifiers must be declared before use.
- Scope is lexical; inner declarations shadow outer ones (variables, types, routines)
- **User-defined types (records, arrays) are reference types**:
    - Variables of these types hold references (pointers) to heap-allocated aggregates.
    - Assignment copies the reference, not the aggregate.

### 2.2 Statements

Supported statements (see `Statement.kt`):

- `Assignment`: `ModifiablePrimary := Expression`
- `RoutineCall` as a statement
- `WhileLoop`: `while Expression loop Body end`
- `ForLoop`: `for Identifier in Range [ reverse ] loop Body end`
- `IfStatement`: `if Expression then Body [ else Body ] end`
- `PrintStatement`: `print Expression { , Expression }`

**Assignment semantics (primitive types)** – simplified summary from the spec:

- `integer := integer` → copy
- `integer := real` → real rounded to nearest integer
- `integer := boolean` → `true` → `1`, `false` → `0`
- `real := real` → copy
- `real := integer` → convert to real (no rounding issues)
- `real := boolean` → `true` → `1.0`, `false` → `0.0`
- `boolean := boolean` → copy
- `boolean := integer` → `1` → `true`, `0` → `false`, otherwise **runtime error**
- `boolean := real` → illegal (semantic analyzer should disallow)

For user-defined types, left and right types must be the *same* declared type; assignment copies references.

The **same rules** apply to parameter passing and routine return values.

### 2.3 Expressions

Grammar (already reflected in the AST):

- `Expression`: `Relation { (and | or | xor) Relation }`
- `Relation`: `Simple [ (< | <= | > | >= | = | /= ) Simple ]`
- `Simple`: `Factor { ( + | - ) Factor }`
- `Factor`: `Summand { ( * | / | % ) Summand }`
- `Summand`: `Primary | ( Expression )`
- `Primary`:
    - `[ Sign | not ] IntegerLiteral`
    - `[ Sign ] RealLiteral`
    - `true` / `false`
    - `[ Sign | not ] ModifiablePrimary`
    - `RoutineCall`
- `ModifiablePrimary`:
    - `Identifier { . Identifier | [ Expression ] }`  
      (variables, fields, array elements)

Language-specific semantics you must respect:

- `and`, `or`, `xor`, `not` are boolean operators on `boolean`.
- Relational operators return `boolean`.
- Arrays are **1-based indexed**.

### 2.4 Loops

**While:** pre-condition loop; condition expression must be boolean-ish and conform to `boolean`.

**For:**

- Syntax: `for Identifier in Range [ reverse ] loop Body end`
- `Range`:
    - `Expression .. Expression` – explicit integer range.
    - `Expression` only – this expression must be an array; loop iterates over elements (`for x in arr` style).
- For numeric range:
    - Loop variable is an implicit `integer` declared in loop scope.
    - On each iteration, loop var is incremented (or decremented if `reverse`) by 1.
    - Body executes zero or more times; if start > end, executes zero times.
- For array range (second expr omitted):
    - Iterates over all elements of the array from first to last element (or reverse when `reverse` is present).
- Loop variable is read-only; assignments to it are not allowed.

### 2.5 Routines

- Top-level only; no nested routines.
- `RoutineDeclaration = RoutineHeader [RoutineBody]`
- `RoutineHeader`: `routine Identifier ( [Parameters] ) [ : Type ]`
- `RoutineBody`:
    - `is Body end` – procedure-like, no return value.
    - `=> Expression` – expression-bodied function (returns that expression).
- There can be forward declarations (header only); semantics enforce matching signatures.

In this project variant, there is **no explicit `return` statement**; returning is only via expression-bodied routines (`=> Expression`). Semantic analysis should ensure that if `returnType` is non-null, body is a single expression body.

The program can conceptually start from *any* routine; we will export all routines as WebAssembly functions.
