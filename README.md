# Компилятор для языка I
- Hand-written parser
- Target platform: WASM

To compile the program use the following command:
```shell
app:run --args="<source-file> [--output <output-file>]"
```

Examples:
```shell
./gradlew app:run --args="../tests/errors/Playground.txt --output output/program.wasm"
```
```shell
./gradlew app:run --args="../samples/sudoku/source --output ../samples/sudoku/program.wasm"
```

After the compilation you need to find the suitable WASM runtime. For example, V8 can execute WASM:
```shell
node app/output/simple_run.js
```

```shell
node samples/sudoku/run.js
```