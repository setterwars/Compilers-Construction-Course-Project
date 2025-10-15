```mermaid
graph TD
    A[ProgramNode]
    B[RoutineDeclarationNode]
    A --> B

%% Routine Header
    C[RoutineHeaderNode]
    B --> C
    C --> C1[TRoutine 'routine']
    C --> C2[TIdentifier 'main']
    C --> C3[TLParen '(']
C --> C4[ParametersNode (empty)]
C --> C5[TRParen ')']

%% Routine Body
D[RoutineBodyNode]
B --> D
D --> D1[TIs 'is']

%% Body -> Statement -> ForLoop
E[BodyNode]
D --> E
F[StatementNode]
E --> F
G[ForLoopNode]
F --> G

%% ForLoop header
G --> G1[TFor 'for']
G --> G2[TIdentifier 'i']
G --> G3[TIn 'in']

%% Range
H[RangeNode]
G --> H

%% Range start expression: 1
I[ExpressionNode]
H --> I
I1[RelationNode]
I --> I1
I2[SimpleNode]
I1 --> I2
I3[FactorNode]
I2 --> I3
I4[SummandNode]
I3 --> I4
I5[PrimaryNode]
I4 --> I5
I6[TIntLiteral '1']
I5 --> I6

%% '..'
H1[TRangeDots '..']
H --> H1

%% Range end expression: 3
J[ExpressionNode]
H --> J
J1[RelationNode]
J --> J1
J2[SimpleNode]
J1 --> J2
J3[FactorNode]
J2 --> J3
J4[SummandNode]
J3 --> J4
J5[PrimaryNode]
J4 --> J5
J6[TIntLiteral '3']
J5 --> J6

%% Loop body
G4[TLoop 'loop']
G --> G4
K[BodyNode]
G --> K
L[StatementNode]
K --> L
M[PrintStatementNode]
L --> M
M1[TPrint 'print']
M --> M1

%% print argument: identifier i
N[ExpressionNode]
M --> N
N1[RelationNode]
N --> N1
N2[SimpleNode]
N1 --> N2
N3[FactorNode]
N2 --> N3
N4[SummandNode]
N3 --> N4
N5[PrimaryNode]
N4 --> N5
N6[ModifiablePrimaryNode]
N5 --> N6
N7[TIdentifier 'i']
N6 --> N7

%% ForLoop end / RoutineBody end
G5[TEnd 'end']
G --> G5
D2[TEnd 'end']
D --> D2

```
