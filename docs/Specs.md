# EBNF Specification of language I (Extended and Improved)
```
Program
  : { SimpleDeclaration | RoutineDeclaration }

SimpleDeclaration
  : VariableDeclaration
  | TypeDeclaration

VariableDeclaration
  : var Identifier : Type [ is Expression ]
  | var Identifier is Expression

TypeDeclaration
  : type Identifier is Type
```
```
Type
  : PrimitiveType
  | UserType
  | Identifier

PrimitiveType
  : integer | real | boolean

UserType
  : ArrayType | RecordType

RecordType
  : record { VariableDeclaration } end

ArrayType
  : array [ [ Expression ] ] Type
  ```
```
Statement
  : Assignment
  | RoutineCall
  | WhileLoop
  | ForLoop
  | IfStatement
  | PrintStatement
  | ReturnStatement

Assignment
  : ModifiablePrimary := Expression

RoutineCall
  : Identifier ( [ Expression { , Expression } ] )

WhileLoop
  : while Expression loop Body end

ForLoop
  : for Identifier in Range [ reverse ] loop Body end

Range
  : Expression [ .. Expression ]

IfStatement
  : if Expression then Body [ else Body ] end

PrintStatement
  : print Expression { , Expression }
  
ReturnStatement
  : return [ Expression ]
```
```
RoutineDeclaration
  : RoutineHeader [ RoutineBody ]

RoutineHeader
  : routine Identifier ( [ Parameters ] ) [ : Type ]

RoutineBody
  : is Body end
  | => Expression

Parameters
  : ParameterDeclaration { , ParameterDeclaration }

ParameterDeclaration
  : Identifier : Type
```
```
Body
  : { SimpleDeclaration | Statement }
 ```
```
Expression
  : Relation { ( and | or | xor ) Relation }

Relation
  : Simple [ ( < | <= | > | >= | = | /= ) Simple ]

Simple
  : Factor { ( + | - ) Factor }

Factor
  : Summand { ( * | / | % ) Summand }

Summand
  : Primary | ( Expression )

Primary
  : [ Sign | not ] IntegerLiteral
  | [ Sign ] RealLiteral
  | true
  | false 
  | [ Sign | not ] ModifiablePrimary
  | RoutineCall 

Sign
  : + | -

ModifiablePrimary
  : Identifier { . Identifier | [ Expression ] }
  
 ```

## Notes:
### Variadic functions and variable-sized arrays
The `array[]` parameter in the function declaration 
simply means "some array pointer with unknown size"

The sizeless array can only be used inside the function parameters
```

### Calling a function that contains record or array 
Since records and arrays are represented as pointers of type i32, one 
can theoretically invoke a function that contains records or arrays
in parameter list as the main function of a compiled program by passing plain integers:
```
type Point is record
    var x: integer
    var y: integer
end
routine main(a: array[5] Point, b: Point)
```
```
wasmtime program.wasm --invoke main 90 605
```


