# Парсер

Запускаем парсер (из корневой папки!)

`./gradlew app:run --args="../tests/worked/<Название файла теста>.txt"`

Пример:
`./gradlew app:run --args="../tests/worked/Fibonacci.txt"`

Запускаем тесты (Из корневой папки! + работает только в unix):
```shell
chmod +x ./scripts/run_all_tests.sh
./scripts/run_all_tests.sh
```
По сути, тестер просто запускает команду ``./gradlew app:run --args="../tests/worked/..."`` для всех файлов из директории
`tests/worked/`

