# Выполнение миграций справочников

Синтаксис выражений вынесен в [migration-syntax.md](migration-syntax.md). Пользовательские миграции `dicts` проходят через `SqlParser` и `Interpreter`, а не исполняются как SQL непосредственно в PostgreSQL. Они применимы и к MongoDB с учётом возможностей backend.

## Файлы и порядок

- Ресурсы располагаются в `src/main/resources/db/migration/dicts/` и имеют расширение `.sql`.
- Используй формат имени `V1_0_1__description.sql`, как в тестовых ресурсах проекта. Версия извлекается `MigrationUtils.parseVersion` из числовых частей с `_` и сравнивается через Spring Data `Version`.
- `DictsDDLProvider` собирает файлы из classpath-каталогов/JAR, включая вложенные каталоги, и сортирует их по версии.
- Имя файла и версия должны быть уникальными среди всех участвующих ресурсов. Внутренний сборщик использует имя без каталога как ключ; одинаковое имя в разных каталогах не изолирует миграции. Для разных имён с одной версией есть отдельная ошибка `MigrationHasSameVersionException`.
- Подкаталоги не задают независимый порядок или отдельные пространства версий.

## Старт приложения и журнал

`DictsDDLProvider` выполняет `update()` из `afterPropertiesSet()`, предварительно инициализируя блокировки справочников. Следовательно, обычный запуск контекста с локальными компонентами может применить новые файлы. Не запускай приложение с новой миграцией как способ «только проверить синтаксис».

Для каждого файла используется путь журнала `db/migration/dicts/<имя-файла>`. `VersionSchemeBackend` хранит версию, путь, время установки и MD5 содержимого скрипта.

- Новый файл передаётся в `ClasspathMigrationService.migrate`.
- Уже применённый файл с совпадающим checksum пропускается.
- Несовпадение checksum вызывает ошибку. Изменения текста, комментариев и переводов строк также меняют содержимое для checksum.

Добавляй новый файл с новой версией, сохраняя применённые миграции неизменными. Не меняй журнал/checksum для обхода ошибки без отдельного решения по исправлению состояния.

## Процесс применения и ограничения

`ClasspathMigrationService` выполняет операцию через `TransactionalExecutor`: открывает `DictTransactionProvider`, разбирает скрипт, валидирует его, исполняет выражения, сохраняет `VersionScheme` и завершает транзакцию. При ошибке вызывается rollback, затем выбрасывается `MigrationAppliedException`.

В исходниках есть явная оговорка о некорректном rollback как минимум для PostgreSQL. Не обещай полную атомарность/автоматическое восстановление DDL. При ошибке проверь фактические схемы, данные и журнал до повторного запуска.

`ClasspathMigrationValidationService` запрещает более одного **различного явно указанного** `engine` в `CREATE TABLE` одного файла. Разделяй изменения разных engine на отдельные миграции и проверяй default engine для выражений без указания `engine`.

`COPY` загружает CSV/JSON из classpath. Ресурс должен попасть в JAR приложения; абсолютный classpath-путь вида `/dicts/data.json` разрешается относительно корня classpath. Это не PostgreSQL `COPY FROM` с серверной файловой системой.

## Как проверить изменение

- Разбор синтаксиса проверяется `SqlParser`; успешный разбор не подтверждает семантику и поддержку backend.
- Интерпретацию и результат проверяй на нужном PostgreSQL/MongoDB backend, учитывая права, обязательные поля, ссылки, значения и ограничения.
- В репозитории компонентов есть `SqlParserTest`, `CommonInterpreterTest`, `CommonClasspathMigrationServiceTest` и backend-наследники. Их интеграционное окружение требует Testcontainers/БД.
- В приложении используй его инфраструктуру тестов и профиль, соответствующий изменяемому справочнику.

## Источники

`dicts/src/main/java/com/backstage/app/dict/`:

- `configuration/ddl/DictsDDLProvider.java`;
- `utils/MigrationUtils.java`;
- `service/migration/ClasspathMigrationService.java`, `DictTransactionProvider.java`;
- `service/validation/ClasspathMigrationValidationService.java`.

Примеры и проверки: `dicts/src/test/resources/db/migration/dicts/`, `dicts/src/test/java/com/backstage/app/dict/service/`.
