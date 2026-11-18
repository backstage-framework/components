# Карта Backstage Components

## Назначение и границы

`components` — набор Java-библиотек платформы Backstage и отдельная сборка Gradle-плагинов. Библиотеки подключаются в целевые Spring Boot приложения. Их группа публикации — `com.backstage`, общая версия берётся из `appVersion`.

Карта составлена по сборочным файлам, исходникам и тестам. Она помогает найти код, но не заменяет проверку конкретного метода и условий активации. При чтении копии из `com.backstage:agents` пути ниже обозначают **исходный репозиторий компонентов**, а не файлы приложения-потребителя.

## Сборка и стек

| Место | Назначение |
| --- | --- |
| `settings.gradle` | Состав подпроектов и `includeBuild 'gradle-plugins'` |
| `build.gradle` | Общие Java/IDE/Checkstyle настройки, toolchain, репозитории, JAR с исходниками/Javadoc, Maven-публикация |
| `gradle.properties` | Версии Backstage, Java, Gradle, плагинов и внешних библиотек, параметры репозитория |
| `gradle/wrapper/` и `gradlew` | Wrapper; используй его для сборки |
| `dependencies/build.gradle` | Публикация Gradle version catalog `com.backstage:dependencies` |
| `gradle-plugins/build.gradle` | Отдельная Groovy-сборка, публикация `com.backstage:gradle-plugins`, регистрация plugin IDs |
| `CHANGELOG.md` | История изменений по компонентам; может отставать от текущих версий сборки |
| `.gitlab-ci.yml`, `Dockerfile` | Docker-in-Docker сборка/публикация; CI запускается для веток по шаблону `r...` |

На момент составления карты в рабочей копии указаны Java 21, Gradle 9.9.1, Backstage `6.0.0-Beta8`, Spring Boot 4.1.2, EclipseLink 5.0.2, jOOQ 3.21.9, Camunda 7.24.0, Groovy 5.1.3, JUnit Jupiter 6.1.3, Testcontainers 1.21.4. Источник актуальных версий — `gradle.properties`, а не этот список. Серверные модули используют `java-library`, Spring Boot dependency management и Lombok; собираются обычные JAR, `bootJar` отключён.

## Модули и зависимости

В колонке зависимостей перечислены непосредственные проектные зависимости `api`. Опциональные `compileOnly` указаны отдельно.

| Модуль | Назначение и основные точки входа | Проектные зависимости |
| --- | --- | --- |
| `app` | Базовая инфраструктура: `AppPackage`, `AppConfiguration`, `AppProperties`; модели пользователя, исключения, конвертеры, утилиты, безопасность, календарь и переводы | — |
| `api` | API-конфигурация, глобальная обработка ошибок MVC, SpringDoc/OpenAPI/Swagger, CAPTCHA, endpoint текущего пользователя и перечислений | `app` |
| `jobs` | Фоновые задания на Spring scheduler: `AbstractJob`, `JobManager`, `JobTrigger`, `JobParams`, `ScheduledJobsEndpoint`, `SchedulerConfiguration` | `api` |
| `database` | DataSource/JDBC, EclipseLink JPA, репозитории, Flyway/DDL, PostgreSQL, ClickHouse, MongoDB, pg_partman | `app`, `jobs` |
| `attachments` | Метаданные и привязки файлов, `AttachmentService`, `AttachmentStore`, `AttachmentEndpoint`; MinIO и файловый каталог | `api`, `jobs`, `database` |
| `jms` | JMS/ActiveMQ, встроенный брокер, `JmsConfiguration`, `JmsEmbeddedBrokerConfiguration`, `JMSEndpoint`, health checks, опциональная Eureka-интеграция | `api`, `app` |
| `audit` | `AuditService`, события и builder, фильтры, `AuditStore`: JPA/JMS/log реализации | `jms`, `database` |
| `cache` | `CacheService`, настройки кешей, JCache/Ehcache/Hazelcast, распределённые операции и endpoint управления | `app`; опционально `jms` |
| `dicts-api` | DTO/контракты справочников, `DictItem`, generic/remote сервисы и endpoint-интерфейсы | `api` |
| `dicts` | Схемы и данные справочников, права, валидация, запросы/DDL, PostgreSQL/MongoDB backend, миграции и импорт/экспорт | `dicts-api`, `database`, `cache`; опционально `attachments` |
| `dicts-codegen` | Генераторы моделей, серверных сервисов/endpoint/advice и клиентских сервисов справочников; общие base-классы | `dicts-api`; опционально `dicts` |
| `bpm` | Абстракция движков процессов, workflow, задачи, скрипты, REST API и хранение метаданных | `database`, `jobs` |
| `bpm-camunda` | Реализация BPM на Camunda и миграции схемы движка | `bpm` |
| `quartz` | Конфигурация Quartz, фабрика Spring jobs и DDL схемы scheduler | `database` |
| `report` | Генерация и очередь отчётов, `ReportService`, `ReportTaskService`, stores в памяти/вложениях, генераторы и утилиты | `api`, `jobs`, `attachments` |
| `feign-session` | Передача контекста запроса/пользователя: `FeignRequestInterceptor`, `FeignSessionRequestFilter`, `PrincipalProvider` | `api` |
| `dependencies` | Публикуемый каталог версий внешних библиотек и плагинов | — |
| `agents` | Публикуемая документация для coding-агентов: `README.md`, `COMPONENTS.md`, эта карта, skill `backstage-dicts` со справочниками | — |

## Общие правила навигации

- Обычная структура модуля: `src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`.
- Основные пакеты — `com.backstage.app.*`. Исключения: `com.backstage.bpm.*`, `com.backstage.feign.*`, `com.backstage.gradle.*`.
- Внутри модуля ищи `configuration` и `configuration/properties`, затем `service`, `endpoint`, `model/domain`, `model/dto`, `repository`, `conversion`. Состав каталогов отличается по модулю.
- `app/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` регистрирует `com.backstage.app.AppPackage`. Он выполняет `@ComponentScan("com.backstage")`.
- `AppProperties.getBasePackages()` добавляет `com.backstage` к `app.base-packages`; эти пакеты используются, в частности, при настройке JPA.
- Многие интеграции включаются условиями `@ConditionalOnProperty`/собственными `@ConditionalOn...`. Проверяй `matchIfMissing`, доступные beans и опциональные зависимости по конкретной конфигурации.

## Основные подсистемы

### Общая инфраструктура и HTTP API

- `app/.../configuration` — контекст приложения, параметры, логирование, условия наличия qualified beans, сессии.
- `app/.../model/other/user` и `service/user` — principal и права; `SecurityUtils` — доступ к текущему пользователю.
- `app/.../conversion/dto` — `Converter`, `AbstractConverter`, конфигурируемые конвертеры и mixin aspect.
- `app/.../utils` — JSON, HTTP/REST, даты/время, Spring context, коллекции, валидация; `utils/transactional` — `TransactionalExecutor`, `TransactionalUtils`.
- `app/src/main/resources/logback*` — общая конфигурация и appenders.
- `api/.../configuration`, `controller/GlobalMvcExceptionHandler`, `endpoint` — конфигурация HTTP API, обработка ошибок и общие endpoint; настройки SpringDoc ищи по именам `SpringDocConfiguration`, `SpringDocWebMvcConfiguration`.
- `api/src/main/resources/application.properties` задаёт базовые настройки SpringDoc/локали/validation. Включение OpenAPI и Swagger регулируется отдельными параметрами `ApiProperties`.

### База данных и миграции

- `database/.../configuration/jpa/DataSourceConfiguration` — `app.datasource`, отдельные no-timeout и DDL DataSource, JDBC templates и transaction manager.
- `configuration/annotation/AppDataSource`, `DDLDataSource` — квалификаторы источников данных.
- `configuration/jpa/JpaConfiguration` — `EclipseLinkJpaVendorAdapter`, entity manager, transaction manager, `CustomJpaRepositoryFactoryBean`.
- `configuration/jpa/customizer` — расширение настройки entity manager, пакетов и mapping resources.
- `configuration/jpa/eclipselink` — UUID, PostGIS, схема таблиц, проверка metamodel и `@ReadOnlyColumn`.
- `repository/CustomJpaRepository*` — общая реализация/фабрика JPA-репозиториев.
- `configuration/ddl` — `DDLProvider`, `AbstractDDLProvider`, `DDLConfiguration`, `DDLProviderInitializer`; `utils/DDLUtils` применяет миграции.
- `src/main/resources/ds.properties`, `jpa.properties`, `flyway.properties` — базовые настройки БД.
- Миграции компонентов находятся в собственных ресурсах: `db/migration/attachments`, `audit`, `bpm`, `bpm_camunda`, `quartz`.
- `database/.../partman` и `PartmanHealthConfiguration` — создание PostgreSQL partitions и проверки состояния pg_partman.

### Справочники

- `dicts-api/.../dict/model`, `service`, `endpoint` — публичная модель и контракты; `GenericDictDataService`, `InternalDictDataService`, `ExternalDictDataService`, `RemoteDictService`.
- `dicts/.../dict/service/DictService`, `DictDataService`, `DictPermissionService` — основные сервисы.
- `service/backend` — контракты `DictBackend`, `DictSchemeBackend`, `DictDataBackend`, `VersionSchemeBackend`, `DictTransactionBackend`; подкаталоги `postgres` и `mongo` — реализации.
- `configuration/backend` и соседние конфигурации выбирают backend по параметрам. `DictsProperties` разделяет storage метаданных, набор engines и default engine.
- `service/ddl/SqlParser`, `Interpreter` и `service/ddl/ast` — язык изменения схем справочников и AST; `service/query/QueryParser` — запросы.
- `service/validation`, `service/lock`, `service/mapping` — проверка схем/данных, блокировки и преобразования.
- `service/migration` — classpath и storage миграции; `service/imp`, `service/export` — CSV/JSON/SQL импорт и экспорт.
- `service/advice` — расширения поведения справочников и интеграция с вложениями.
- `dicts-codegen/.../service/codegen/server` — генераторы и base-классы сервера; `client` — генераторы и base-классы клиента. Генерация запускается в контексте приложения, поскольку требует схем справочников.

### Задания, BPM, вложения и отчёты

- `jobs/.../service` — `AbstractJob`, `JobManager`, параметры/результаты, описание заданий; `configuration/SchedulerConfiguration` создаёт scheduler. Это отдельный механизм от Quartz.
- `bpm/.../service/process` — `ProcessEngine`, `AbstractProcessEngine`, `ProcessService`.
- `bpm/.../service/workflow` — `WorkflowService`, `WorkflowProvider`, classpath/database providers.
- `bpm/.../service/task/TaskManager`, `service/script/ScriptManager` — пользовательские задачи и выполнение скриптов.
- `bpm-camunda/.../configuration/CamundaConfiguration` и реализации движка/knowledge service — интеграция Camunda.
- `attachments/.../service/AttachmentService` — создание, изменение, чтение, привязка/release и удаление вложений; stores отвечают за содержимое, репозитории — за метаданные.
- `report/.../service/ReportService`, `AttachmentReportService`, `InMemoryReportService` — внешний интерфейс генерации/хранения; `service/store` — stores; task/queue сервисы — выполнение в фоне.
- `audit/.../service/AuditService` направляет события в `AuditStore`; `JpaAuditStore`, `JmsAuditStore`, `LogAuditStore` — разные способы записи.

## Параметры конфигурации

Это точки входа для поиска, а не полный список и не обещание включения по умолчанию. Значения и обязательные поля смотри в соответствующем `*Properties` и условиях конфигурации.

| Модуль | Префиксы / значимые свойства |
| --- | --- |
| `app` | `app`, `app.logging`, `app.business-calendar` |
| `api` | `app.api`, `app.api.openapi.enabled`, `app.api.swagger.enabled`, `app.captcha.enabled` |
| `database` | `app.datasource`, `app.notimeoutdatasource`, `app.ddldatasource`, `app.jpa.enabled`, `app.jpa.validate-meta-model`, `app.ddl`, `app.transaction`, `app.mongo`, `app.clickhouse.host`, `app.partman.enabled` |
| `attachments` | `app.attachments.enabled`, `app.attachments.minio.endpoint`, `app.attachments.directory.path` |
| `audit` | `app.audit.enabled` |
| `jobs` | `app.scheduler.enabled` |
| `jms` | `app.jms.enabled` |
| `cache` | `app.cache.enabled`, `app.cache.distributed-operations` |
| `dicts` | `app.dicts.enabled`, `app.dicts.api-enabled`, `app.dicts.storage`, `app.dicts.engines`, `app.dicts.default-engine` |
| `dicts-api` | `app.dicts.public-api.enabled` |
| `bpm` | `app.bpm` |
| `quartz` | `app.quartz.enabled` |
| `report` | `app.report.enabled`, `app.report.store-type` |
| `feign-session` | `app.feign`, `app.feign.propagate-request-session` |

## Gradle-плагины

Классы находятся в `gradle-plugins/src/main/groovy/com/backstage/gradle/`. Регистрация ID — в `gradle-plugins/build.gradle`.

| Plugin ID | Класс | Поведение |
| --- | --- | --- |
| `backstage-jpa-persistence-xml` | `JpaPersistenceXmlPlugin` | После `classes` находит entity-классы и генерирует `build/resources/main/db/META-INF/persistence.xml`; extension `jpaPersistenceXml` |
| `backstage-jpa-weaving` | `JpaWeavingPlugin` | Выполняет EclipseLink static weaving после `classes`; extension `jpaWeaving` |
| `backstage-jpa-modelgen` | `JpaModelGenPlugin` | Подключает metamodel annotation processor и передаёт путь `persistence.xml`; extension `jpaModelGen` |
| `backstage-app-version` | `AppVersionPlugin` | Записывает `app.version`, `app.module` в `src/main/resources/config/application.properties` |
| `backstage-bpmn-pack` | `BpmnPackPlugin` | Задача `pack` экспортирует classpath workflow в JSON; extension `bpmn` |
| `backstage-checkstyle` | `CheckstylePlugin` | Checkstyle, `checkstyleApplyConfig`, pre-commit hook; настройки в ресурсах `checkstyle/` |
| `backstage-dicts-codegen` | `DictsCodegenPlugin` | Задача `dictsCodegen` на базе `bootRun`; extension `dictsCodegen` задаёт target project/package/version |
| `backstage-dependency-versions` | `DependencyVersionsPlugin` | Задача `updateDependencyVersions` обновляет версии в `gradle.properties` в пределах major |
| `backstage-agent-docs` | `AgentDocsPlugin` | Установка документации и обновление `AGENTS.md` при конфигурации приложения; конфигурация/extension `agentDocs` |

`writeProperties` в сборке плагинов создаёт ресурс `backstage.properties` с версией Backstage. Её используют плагины, которым нужна версия компонентов по умолчанию. Версия приложения-потребителя и версия Backstage могут различаться.

## Доставка документации агентам

- Источники: `agents/src/main/resources/README.md`, `COMPONENTS.md`, `skills/backstage-dicts/`; `agents:processResources` добавляет корневой `CODEMAP.md`. Основные документы именуются заглавными буквами; тематические справочники — lowercase-kebab-case.
- Публикация: обычный JAR `com.backstage:agents:<appVersion>` через общую Maven publication; runtime-зависимостей нет.
- Потребитель применяет `backstage-agent-docs` из `com.backstage:gradle-plugins`; плагин устанавливает документацию на фазе конфигурации, после обработки `build.gradle`. Это работает при импорте Gradle-проекта в IDE или запуске любой команды Gradle, включая `help`.
- Отдельной задачи `syncAgentDocs` и зависимостей lifecycle/упаковки от неё нет. Плагин не подключает `base`.
- Конфигурация `agentDocs` является resolvable, non-consumable, non-transitive. По умолчанию получает `com.backstage:agents:<version>`, явная зависимость заменяет default.
- Приоритет версии: `agentDocs.version` → Gradle property `backstageVersion` → ресурс плагина `backstage.properties`.
- Для совместного обновления библиотек и документации используй общее свойство `backstageVersion`, не фиксируя `agentDocs.version`. При смене версии следующий импорт/запуск Gradle загружает соответствующий артефакт.
- `agentDocs.outputDirectory` по умолчанию равен `layout.projectDirectory.dir('.gradle/backstage/agent-docs')`. Перед `FileSystemOperations.sync` плагин проверяет, что выходной каталог выделен под документацию, и помечает его файлом `.backstage-agent-docs`. Прежние установки распознаются по каталогу и ссылке в управляемом блоке. Размещение вне `build` сохраняет документацию после `clean`.
- До записи плагин проверяет наличие `README.md` или старого `index.md` в корне архива, маркеры блока `AGENTS.md` и конфликты с пользовательскими skills. Ссылка в `AGENTS.md` указывает на найденный входной документ.
- Из каталога `skills` плагин устанавливает отдельные skills в корневой `.agents/skills/<имя>`. Каталоги помечаются `.backstage-agent-docs`; обновление/удаление касается только управляемых skills. Неуправляемый каталог с совпадающим именем сохраняется, конфигурация завершается ошибкой. Создание, обновление, удаление и восстановление skills проверяются функциональными тестами, включая configuration cache и применение в подпроекте.
- После извлечения плагин создаёт или обновляет отдельный блок Backstage в корневом `AGENTS.md` приложения. HTML-маркеры ограничивают заменяемый текст; остальные инструкции и существующие переносы строк сохраняются. Ссылка учитывает `outputDirectory` и путь Gradle-проекта и направляет к нужному skill/модулю без обязательного чтения всех документов; старое требование выполнить `syncAgentDocs` удаляется из управляемого блока.
- `skills/backstage-dicts/SKILL.md` — короткая точка входа. Его `references` содержат возможности/API, конфигурацию, выполнение и синтаксис миграций, язык фильтров. Материалы сверяются с `SqlParser`, `Interpreter`, `QueryParser`, миграционными сервисами и тестами. Cookbook не включён.
- При повреждённых или повторяющихся маркерах `AGENTS.md` не изменяется, конфигурация завершается ошибкой. Установленные Markdown-файлы и `AGENTS.md` читаются после обновления для отслеживания изменений при использовании configuration cache.
- Артефакт документации должен быть доступен на фазе конфигурации: опубликован в Maven или уже собран в локальный JAR. В текущем репозитории плагин не применяется к самому себе; корневой блок `agentDocs project(':agents')` удалён.
- Автоматическое обнаружение файлов в JAR не предполагается. Плагин сам добавляет в `AGENTS.md` ссылку на извлечённую документацию; пример блока находится в `README.md`.

## Тесты и проверка

- Java-тесты используют JUnit Platform, Spring Boot Test, во многих модулях — Testcontainers. Ищи `AbstractTests`/`AbstractTest`, `TestApp`/`TestApplication`, initializers и `src/test/resources/application.yml`.
- `database` поднимает PostgreSQL и ClickHouse; `attachments` — PostgreSQL и MinIO; `audit`, `cache`, `bpm-camunda`, `report` используют PostgreSQL; `dicts` проверяет PostgreSQL и MongoDB и содержит интеграцию с MinIO; `jms` содержит тест внешнего ActiveMQ.
- В `dicts` есть общие наборы тестов и отдельные `service/postgres`, `service/mongo`; изменение общей логики требует проверки обеих реализаций.
- `dicts-codegen` содержит `ModelNameUtilsTests`; `api` — тесты обработчиков исключений и Swagger; `app` — тесты конвертеров, REST и перечислений.
- `gradle-plugins/src/test/groovy/.../AgentDocsPluginTests` использует Gradle TestKit и локальный Maven-репозиторий: проверяет установку при конфигурации без отдельной задачи, смену версии и удаление старой документации, prebuilt JAR, configuration cache, восстановление файлов, сохранение пользовательских инструкций и CRLF, миграцию старого блока, обработку повреждённых маркеров, применение плагина в подпроекте и сохранность документации после `clean build`.
- Команды: `./gradlew :<module>:test`, `./gradlew :<module>:test --tests '<pattern>'`, `./gradlew :<module>:checkstyleMain`, `./gradlew -p gradle-plugins test validatePlugins`, `./gradlew :agents:jar`.
- Полная `./gradlew check` требует зависимостей и инфраструктуры всех модулей. Dockerfile запускает публикацию, а не обычную локальную проверку; для проверки изменений его запускать не нужно.
