# Парсер

**Запускаем парсер (из корневой папки!):**
`./gradlew :app:run --args="<путь к файлу> [--file=PATH] [--truncate=true|false] [--max-depth=INT]"`

Примеры:

`./gradlew app:run --args="../tests/worked/Fibonacci.txt"`

**Если нужно сгенерировать mermaid диаграмму, то запускать**

`./gradlew app:run --args="../tests/worked/Fibonacci.txt --file=../output/FibonacciMermaid.md --truncate=true"`

Больше примеров:
- `./gradlew app:run --args="../tests/worked/APlusB.txt --file=../output/APlusB.md --truncate=false"`
- `./gradlew app:run --args="../tests/worked/APlusB.txt --file=../output/APlusB.md --truncate=true"`

- `./gradlew app:run --args="../tests/worked/While.txt --file=../output/While.md --truncate=false"`
- `./gradlew app:run --args="../tests/worked/While.txt --file=../output/While.md --truncate=true"`

- `./gradlew app:run --args="../tests/worked/EuclideanDistance.txt --file=../output/EuclideanDistance.md --truncate=false"`
- `./gradlew app:run --args="../tests/worked/EuclideanDistance.txt --file=../output/EuclideanDistance.md --truncate=true"`
- `./gradlew app:run --args="../tests/worked/EuclideanDistance.txt --file=../output/EuclideanDistance.md --truncate=false --max-depth=3"`

Если диаграмма слишком большая, то посмотреть можно на https://www.mermaidflow.app/editor

Запускаем тесты (Из корневой папки! + работает только в unix):
```shell
chmod +x ./scripts/run_all_tests.sh
./scripts/run_all_tests.sh
```
По сути, тестер просто запускает команду ``./gradlew app:run --args="../tests/worked/..."`` для всех файлов из директории
`tests/worked/`

