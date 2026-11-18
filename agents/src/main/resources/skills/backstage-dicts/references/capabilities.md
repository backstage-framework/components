# Возможности и API справочников

## Модули и модель

| Модуль | Назначение |
| --- | --- |
| `com.backstage:dicts-api` | DTO, endpoint-контракты, generic и remote сервисы; подходит клиенту без локального хранилища |
| `com.backstage:dicts` | Схемы, данные, права, миграции, импорт/экспорт, backend PostgreSQL/MongoDB |
| `com.backstage:dicts-codegen` | Генерация типизированных моделей и сервисов, серверных endpoint/advice или удалённых клиентов |

`Dict` описывает справочник: идентификатор, название, engine, поля, enum, индексы, ограничения, права и лимит истории. `DictField` описывает поле: тип, обязательность, множественность, значение по умолчанию, размеры и ссылку на другой справочник/enum.

Типы модели `DictFieldType`: `INTEGER`, `SERIAL`, `DECIMAL`, `STRING`, `BOOLEAN`, `DICT`, `DATE`, `TIMESTAMP`, `JSON`, `ENUM`, `ATTACHMENT`, `GEO_JSON`. Имена типов DSL отличаются: см. [синтаксис миграций](migration-syntax.md).

`DictItem` содержит данные и служебные атрибуты `id`, `version`, `created`, `updated`, `history`. Их жизненным циклом управляет библиотека. Для создания/обновления серверный API принимает `DictDataItem`; DTO из `dicts-api` не являются взаимозаменяемыми с этой серверной моделью.

## Основные серверные сервисы

| Задача | API и особенности |
| --- | --- |
| Получить/проверить схему | `DictService.getById`, `getAll`, `existsById`, `getNativeScheme` |
| Создать/изменить/удалить справочник | `DictService.create`, `update`, `delete`; для поставляемой схемы обычно оформляется новая миграция |
| Изменить поле | `DictService.update`, `renameField`, `restartSerialField` |
| Управлять enum | `createEnum`, `updateEnum`, `deleteEnum` |
| Управлять индексами/уникальностью | `createIndex`, `deleteIndex`, `createConstraint`, `deleteConstraint` |
| Прочитать данные | `DictDataService.getById`, `getByIds`, `getByFilter`, `streamByFilter` |
| Получить идентификаторы/агрегаты | `getIdsByFilter`, `getDistinctValuesByFilter`, `existsById`, `existsByFilter`, `countByFilter` |
| Создать записи | `create(DictDataItem)`, `createMany(dictId, List<DictDataItem>)` |
| Обновить запись | `update(itemId, DictDataItem, version)`; версия проверяется для optimistic locking |
| Массово изменить/удалить | `updateByFilter`, `delete`, `deleteAll`; проверь область действия фильтра до выполнения |

У многих методов есть перегрузки с `userId`; варианты без него используют текущий контекст пользователя. `DictPermissionService` проверяет права просмотра/изменения справочника. Не заменяй обращение к сервису прямым SQL/Mongo-запросом: это меняет семантику прав, валидации и событий.

Фильтры передаются строкой DSL, выбираемые поля — отдельным списком, пагинация/сортировка — через Spring Data `Pageable`/`Sort`. Для полного набора полей используется `*`; сигнатуры и смысл пустого списка сверяй с нужным методом. Синтаксис фильтра описан [отдельно](query-syntax.md).

## Импорт, экспорт и интеграции

- `ImportCsvService`, `ImportJsonService` импортируют данные из `InputStream` или classpath-ресурса. Путь строковой перегрузки обрабатывается через `getResourceAsStream`, а не как произвольный путь файловой системы.
- `DictExportService` поддерживает CSV, JSON и SQL, экспорт по идентификаторам или фильтру и проверку прав просмотра.
- `DictServiceAdvice`, `DictDataServiceAdvice` — точки расширения. Интеграция с вложениями вынесена в `AttachmentDictDataServiceAdvice`; для неё нужен соответствующий модуль и его настройки.
- Кешируются метаданные справочников, схем и полей; операции изменения должны проходить через штатный API, чтобы сохранялась инвалидация кеша.
- `GenericDictDataService`, `InternalDictDataService`, `ExternalDictDataService`, `RemoteDictService` в `dicts-api` предназначены для endpoint/remote сценариев. Выбор локального и удалённого сервиса определяется архитектурой приложения.

## Кодогенерация и backend

Серверная генерация создаёт модели, сервисы, endpoint и advice; клиентская — модели и удалённые сервисы. Для расширения используй предусмотренные base/advice-классы, учитывая, какие файлы пересоздаёт генератор.

Storage метаданных и engine данных — разные настройки. Поддерживаются PostgreSQL и MongoDB, справочник хранит собственный engine. Смена настройки `default-engine` не переносит существующие данные; для переноса предусмотрен `DictStorageMigrationService`. Такой перенос — отдельная операция, а не побочный эффект настройки.

Поддержку конкретного типа или оператора проверяй в backend и тестах. Ограничения перевода, в том числе для логического `NOT`, описаны в [справочнике фильтров](query-syntax.md).

## Исходники для уточнения контракта

Пути относятся к исходному репозиторию Backstage Components:

- `dicts/src/main/java/com/backstage/app/dict/service/DictService.java`, `DictDataService.java`, `DictPermissionService.java`.
- `dicts-api/src/main/java/com/backstage/app/dict/api/domain/DictFieldType.java`, `api/service/`, `api/service/remote/`.
- `dicts/src/main/java/com/backstage/app/dict/service/imp/`, `export/`, `advice/`, `backend/`.
- `dicts-codegen/src/main/java/com/backstage/app/dict/service/codegen/server/` и `client/`.
- Проверки: `CommonDictServiceTest`, `CommonDictDataServiceTest`, `CommonDictDataValidationServiceTest`, `CommonDictExportServiceTest`, их наследники в `service/postgres` и `service/mongo`.
