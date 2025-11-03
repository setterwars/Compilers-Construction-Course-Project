Запускаем тесты (Из корневой папки! + работает только в unix):
```shell
chmod +x ./scripts/run_all_tests.sh
./scripts/run_all_tests.sh
```
По сути, тестер просто запускает команду ``./gradlew app:run --args="../tests/worked/..."`` для всех файлов из директории
`tests/worked/`

