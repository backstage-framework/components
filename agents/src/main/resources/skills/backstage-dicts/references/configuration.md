# Настройка справочников

## Выбор хранилищ

`DictsProperties` использует префикс `app.dicts`. Настройки применяются к локальной реализации `dicts`; для удалённого клиента основными являются контракты и конфигурация `dicts-api`.

| Свойство | Смысл / исходное значение |
| --- | --- |
| `app.dicts.storage` | Обязательное имя backend метаданных |
| `app.dicts.engines` | Обязательный непустой набор backend, доступных для хранения данных |
| `app.dicts.default-engine` | Обязательный engine для новых справочников без явного `engine` в DSL |
| `app.dicts.api-enabled` | Локальный REST API; исходное значение `true` |
| `app.dicts.max-history` | Общий лимит истории, исходное значение `32`, минимум `0`; у справочника может быть собственный `maxHistory` |
| `app.dicts.ddl` | Настройки DDL storage, включая схему PostgreSQL; это не путь пользовательских DSL-миграций |
| `app.dicts.enabled` | Поле properties с исходным значением `false`; фактическую активацию проверяй по условиям beans, не полагайся только на этот флаг |

Имена встроенных backend — `postgres`, `mongo`. `@ConditionalOnEngine` выбирает их по наличию имени в `app.dicts.engines`. Backend для `storage` должен присутствовать среди доступных реализаций.

Минимальная форма выбора одного backend (дополнительно требуются источник данных и общие настройки приложения):

```yaml
app:
  dicts:
    storage: postgres
    engines: postgres
    default-engine: postgres
```

Для нескольких engine используй строку через запятую (`engines: mongo, postgres`), как в тестовой конфигурации. `OnEngineCondition` читает значение `app.dicts.engines` непосредственно из Spring `Environment` и ожидает, что оно преобразуется в `String[]`. Storage и default engine могут различаться. Ограничения на сочетание engine в одном файле описаны в [порядке выполнения миграций](migrations.md).

## Источники данных

- PostgreSQL использует `@DictsPostgresDataSource` и `@DictsPostgresJdbcTemplate`. Если специализированный bean не задан, `DictsConfiguration` берёт доступный `DataSource`; без источника конфигурация завершается ошибкой. При нескольких DataSource определи нужный bean явно по принятому в приложении соглашению.
- MongoDB использует `@DictsMongoTemplate`. Если специализированный bean не задан, используется доступный `MongoTemplate`; Mongo-зависимость и подключение должны быть настроены в приложении.
- Конфигурация обычных JPA-сущностей (`app.jpa`, EclipseLink) и storage справочников — связанные, но отдельные механизмы. Не переноси автоматически Hibernate/JPA mapping на DSL справочников.

## REST и удалённые клиенты

`app.dicts.api-enabled` относится к локальным endpoint из `dicts`. `app.dicts.public-api.enabled` относится к публичным endpoint в `dicts-api` и по умолчанию равен `false`. Это отдельные настройки; проверь, какой набор endpoint нужен приложению.

`DictsRemoteConfiguration` включает Feign-клиенты в `com.backstage`. Для remote приложения проверь маршруты/настройки сервиса и передачу пользователя согласно существующей конфигурации. Наличие `dicts-api` не означает наличия локального `DictService` и backend.

## Кодогенерация

Плагин `backstage-dicts-codegen` настраивается через `dictsCodegen` (`targetProject`, `targetPackage`, `version`) и создаёт задачу `dictsCodegen` на базе `bootRun`. Он передаёт `app.dicts.codegen.outputPath` и `app.dicts.codegen.targetPackage`.

`DictCodegenAppListener` запускается при наличии `app.dicts.codegen.outputPath`: при наличии bean `dictService` выбирает серверную генерацию, иначе — клиентскую. После генерации приложение завершается. Поэтому этот параметр не должен случайно попадать в обычный профиль запуска.

Генерация поднимает контекст приложения; конфигурация источников данных, доступ к схемам справочников и миграции действуют как при обычном старте. Перед запуском выбери подходящий профиль/окружение. Файлы, которые генератор перезаписывает, расширяй через предусмотренные advice/base-классы.

## Источники

- `dicts/.../configuration/properties/DictsProperties.java`, `configuration/DictsConfiguration.java`, `configuration/backend/AbstractDictConfiguration.java`, `configuration/conditional/OnEngineCondition.java`.
- `dicts-api/.../api/configuration/properties/DictsRemoteEndpointProperties.java`, `DictsRemoteConfiguration.java`.
- `gradle-plugins/.../DictsCodegenPlugin.groovy` и `dicts-codegen/.../configuration/DictCodegenAppListener.java`.

Здесь и в других справочниках `...` сокращает пакет `src/main/java/com/backstage/app/dict/` (для Gradle-плагина — `src/main/groovy/com/backstage/gradle/`).
