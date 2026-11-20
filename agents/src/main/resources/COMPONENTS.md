# Использование компонентов Backstage

Это краткий навигатор по интеграции библиотек в целевое приложение. Читай только раздел нужного компонента. Для справочников предназначен skill [backstage-dicts](skills/backstage-dicts/SKILL.md); полная карта исходного репозитория — [CODEMAP.md](CODEMAP.md).

## Общие компоненты и HTTP API

- `app` содержит общие модели, исключения, конвертеры, безопасность, транзакционные и прочие утилиты. Перед добавлением аналога проверь существующие классы.
- `AppPackage` регистрируется через Spring Boot auto-configuration imports и сканирует `com.backstage`. `AppProperties` задаёт `app.base-packages` и добавляет пакет компонентов к пакетам приложения.
- Для прав пользователя используй действующую интеграцию приложения с `Principal`, `PermissionService`, `SecurityUtils`; её нельзя заменять предположением о текущем пользователе.
- Для DTO доступны `Converter`, `AbstractConverter`, конфигурируемые конвертеры и mixin. Следуй принятому в приложении способу преобразования.
- `api` предоставляет обработку ошибок MVC, инфраструктуру OpenAPI/Swagger, CAPTCHA и общие endpoint. Настройки ищи в `ApiProperties` (`app.api`) и `CaptchaProperties` (`app.captcha`).
- В этой линии компонентов используется Jackson 3 (`tools.jackson.*`); аннотации могут оставаться в `com.fasterxml.jackson.annotation`. Копируй импорты по фактическому API приложения.

## База данных

- JPA-провайдер компонентов — EclipseLink. Не полагайся на Hibernate-specific поведение без проверки конфигурации приложения.
- `app.datasource` — основной источник данных, `app.ddldatasource` — DDL, `app.notimeoutdatasource` — отдельный источник для операций без обычного таймаута. Используй существующие qualifiers `@AppDataSource`/`@DDLDataSource` и transaction managers.
- `JpaConfiguration` поддерживает `EntityManagerFactoryCustomizer`, собственную фабрику репозиториев и дополнительные пакеты сущностей; условия работы регулирует `app.jpa`.
- При добавлении сущностей проверь настройки `backstage-jpa-persistence-xml`, `backstage-jpa-weaving`, `backstage-jpa-modelgen` в приложении. Генерируемые XML и metamodel не редактируются вручную.
- Для DDL предусмотрены `DDLProvider`/`AbstractDDLProvider` и упорядоченная инициализация. Новые изменения оформляй миграциями по соглашениям приложения; используй его схему и порядок выполнения, а не схему соседнего компонента.
- MongoDB и ClickHouse настраиваются отдельно через `app.mongo`, `app.clickhouse`; их наличие не следует из одного факта подключения JPA.

## Справочники

Используй [skill backstage-dicts](skills/backstage-dicts/SKILL.md). Он направляет к описанию возможностей/API, настройке, жизненному циклу и синтаксису миграций, языку фильтров. Полный набор справочников читать для одной задачи не требуется.

`dictsCodegen` может форматировать сгенерированные классы через IntelliJ CLI по `.editorconfig` приложения-потребителя. Условия запуска и настройки описаны в [справочнике конфигурации](skills/backstage-dicts/references/configuration.md#кодогенерация).

## Вложения, аудит и отчёты

- `attachments`: используй `AttachmentService` для файлов, метаданных, bind/release и удаления; storage реализуется через `AttachmentStore`. Проверь `app.attachments` и настроенный MinIO/файловый каталог.
- Прямое удаление файла из storage может оставить метаданные и привязки, поэтому операции должны соответствовать модели сервиса и настройкам приложения.
- `audit`: передавай события через `AuditService`, `AuditEvent`/builder; способ записи выбирается существующими `AuditStore` (JPA, JMS, log). Проверь `app.audit` и транзакции целевой операции.
- `report`: используй `ReportService`, task/queue сервисы, предусмотренные генераторы и stores. `app.report.store-type` выбирает хранение в памяти или вложениях. Генерация во вложения связана с транзакциями и доступностью attachment после commit.

## Фоновые задания и BPM

- `jobs` предоставляет `AbstractJob`, `JobManager`, `JobParams`, триггеры и endpoint управления. Настройки — `app.scheduler`. Следуй существующей модели параметров, валидации, планирования и результатов.
- `quartz` — отдельная интеграция с Quartz и собственным DDL; не подменяй ею обычное задание Spring scheduler без требования приложения.
- `bpm`: используй `ProcessService`, `WorkflowService`, `TaskManager` и интерфейс `ProcessEngine`. Workflow могут поставляться из classpath или БД.
- `bpm-camunda` реализует интеграцию Camunda. Перед добавлением специфичного вызова движка проверь, доступен ли нужный сценарий через общий BPM API.
- `backstage-bpmn-pack`/задача `pack` экспортирует classpath workflow в JSON; это отдельная операция от запуска процесса и изменения схемы БД.

## Кеш, JMS и Feign

- `cache`: используй `CacheService`, провайдеры настроек и предусмотренные операции инвалидации. Проверь `app.cache` и реализацию JCache/Ehcache/Hazelcast, принятую в приложении.
- Для распределённых операций кеша учитывай `DistributedCacheService`, настройки `distributed-operations` и наличие JMS. Локальный и распределённый кеш могут иметь разные ограничения и поведение.
- `jms`: настройки `app.jms`, встроенный/внешний ActiveMQ, обработчики и health checks. До изменения destination/формата сообщений проверь производителей и потребителей.
- `feign-session`: `FeignRequestInterceptor`, `FeignSessionRequestFilter`, `PrincipalProvider` передают контекст запроса/пользователя. Проверь `app.feign.propagate-request-session` и политику приложения для конкретного внешнего сервиса.

## Проверка изменений

Используй сборку и тесты целевого приложения. Многие тесты компонентов зависят от PostgreSQL, MongoDB, ClickHouse, MinIO или ActiveMQ; часть использует Testcontainers. Если нужная инфраструктура недоступна, сообщи об ограничении и перечисли фактически выполненные проверки.
