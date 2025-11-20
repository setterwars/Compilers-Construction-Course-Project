Запускаем тесты (Из корневой папки! + работает только в unix):
```shell
chmod +x ./scripts/run_all_tests.sh
./scripts/run_all_tests.sh
```
По сути, тестер просто запускает команду ``./gradlew app:run --args="../tests/worked/..."`` для всех файлов из директории
`tests/worked/`

```shell
`./gradlew app:run --args="../tests/errors/Playground.txt --semantic"
```

Запускаем компилятор:
- `./gradlew app:run --args="../tests/worked/EuclideanDistanceInteger.txt --semantic"`
- После запуска, будет сгенерирован файл `app/output/program.wasm`
- Далее делаем с этим файлом что хотим (например, `node run.js` )