# Парсер

**Запускаем парсер (из корневой папки!):**
`./gradlew :app:run --args="<Path to file> [<Path to mermaid diagram> [<need truncation?>]]""`

Примеры:

`./gradlew app:run --args="../tests/worked/Fibonacci.txt"`

**Если нужно сгенерировать mermaid диаграмму, то запускать**

`./gradlew app:run --args="../tests/worked/Fibonacci.txt ../output/FibonacciMermaid.md true"`

Больше примеров:
- `./gradlew app:run --args="../tests/worked/APlusB.txt ../output/APlusB.md false"`
- `./gradlew app:run --args="../tests/worked/APlusB.txt ../output/APlusB.md true"`

- `./gradlew app:run --args="../tests/worked/While.txt ../output/While.md false"`
- `./gradlew app:run --args="../tests/worked/While.txt ../output/While.md true"`

- `./gradlew app:run --args="../tests/worked/EuclideanDistance.txt ../output/EuclideanDistance.md false"`
- `./gradlew app:run --args="../tests/worked/EuclideanDistance.txt ../output/EuclideanDistance.md true"`

Если диаграмма слишком большая, то посмотреть можно на https://www.mermaidflow.app/editor

Запускаем тесты (Из корневой папки! + работает только в unix):
```shell
chmod +x ./scripts/run_all_tests.sh
./scripts/run_all_tests.sh
```
По сути, тестер просто запускает команду ``./gradlew app:run --args="../tests/worked/..."`` для всех файлов из директории
`tests/worked/`

