# Синтаксис DSL миграций

Это справочник `SqlParser`/`Interpreter` модуля `dicts`, не общий справочник SQL. Операторы завершаются `;`. Ключевые слова разбираются без учёта регистра. Идентификаторы — слова парсера, также допускается форма `"identifier"`; строки — в одинарных кавычках. Поддерживаются SQL-комментарии, как в ресурсах тестов.

Ниже `{...}` обозначает подставляемое значение, `[...]` в описании формы — необязательную часть. В реальном DSL квадратные скобки используются также для подписей и массивов, как показано в SQL-блоках.

## Создание справочника и поля

```sql
create table dictId['Название справочника'] (
	name['Название поля'] text,
	age int,
	roles text[],
	owner text not null references users,
	active bool default true
) engine = 'postgres';
```

Подписи и `engine` необязательны; при отсутствии `engine` используется `app.dicts.default-engine`. Ссылка `references users` указывает на `id` другого справочника. `id` и остальные служебные поля создаёт библиотека, обычно их не объявляют в списке полей.

Порядок частей определения поля: идентификатор, необязательная подпись `['...']`, тип, необязательное `[]`, затем `not null`, `references <dictId>`, `default <value>` — в таком порядке.

| DSL-тип | Тип модели |
| --- | --- |
| `int`, `serial`, `decimal` | `INTEGER`, `SERIAL`, `DECIMAL` |
| `text` | `STRING` |
| `bool`, `boolean` | `BOOLEAN` |
| `date`, `timestamp` | `DATE`, `TIMESTAMP` |
| `json`, `attachment`, `geo_json` | `JSON`, `ATTACHMENT`, `GEO_JSON` |
| Имя enum | `ENUM`, идентификатор берётся из имени типа |

Неизвестное имя типа трактуется как enum, поэтому опечатка не обязательно станет синтаксической ошибкой. `varchar` присутствует в `ColumnType`, но его преобразование в `DictFieldType` в текущем коде не реализовано: используй `text`. Формы PostgreSQL `varchar(255)`, `numeric(10,2)` этим парсером не описаны.

## Изменение схемы

Формы операторов (подставь идентификаторы и значения своей схемы):

```sql
alter table dictId add column fieldId['Название'] text;
alter table dictId drop column fieldId;
alter table dictId rename column oldId to newId['Новое название'];

alter table dictId alter column fieldId set not null;
alter table dictId alter column fieldId drop not null;
alter table dictId alter column fieldId set default 'value';
alter table dictId alter column fieldId set minSize = 1;
alter table dictId alter column fieldId set maxSize = 100;
alter table dictId alter column serialField restart with 100;

alter table dictId set readPermission = 'DICT_READ';
alter table dictId set writePermission = null;
alter table dictId set maxHistory = 64;
drop table dictId;
```

Каждый `ALTER TABLE` содержит одну операцию. Для параметров справочника `readPermission`, `writePermission`, `maxHistory` и для `minSize` и `maxSize` поля используется `=`. Значение по умолчанию меняется командой `set default <value>` без `=`.

При `set not null` интерпретатор проверяет существующие `null`: если задан default, обновляет такие значения, иначе при их наличии выдаёт ошибку. Универсального `ALTER COLUMN TYPE`, `DROP DEFAULT`, `RENAME TABLE` и произвольного SQL в грамматике нет.

## Enum, индексы и ограничения

```sql
create enum status['Статус'] for dictId as ('NEW', 'DONE');
alter table dictId add column state status;
alter table dictId alter enum status add value 'ARCHIVED';
alter table dictId drop enum status;

create index byName on dictId (name);
create index byAge on dictId (age) desc;
drop index byName on dictId;

alter table dictId add constraint uniqueName unique (name, state);
alter table dictId drop constraint uniqueName;
```

Enum принадлежит справочнику; его значения должны быть уникальны. Индексы и уникальные ограничения — разные конструкции: `create unique index` не входит в грамматику. Для индекса направление задаётся завершающим `desc`, а не отдельным направлением каждого поля. Идентификаторы создаваемых enum/индекса не должны совпадать с идентификатором справочника.

## Изменение данных и COPY

```sql
insert into dictId (id, name) values ('first', 'Первый'), ('second', 'Второй');
update dictId set name = 'Изменённый' where id = 'first';
update dictId set name = sourceName where id = 'second';
delete from dictId where id = 'second';
copy dictId from '/dicts/data.csv';
```

Для `INSERT` список полей необязателен; без него используется порядок data fields схемы. Предпочитай явный список, чтобы изменение схемы не меняло соответствие значений. Количество значений должно совпадать с количеством полей.

`WHERE` в миграциях поддерживает одно сравнение (`=`, `!=`, `<>`, `<`, `>`, `<=`, `>=`). В отличие от языка фильтров, здесь нет комбинаций `AND`/`OR`, `IN`, `LIKE`. Правая часть сравнения при исполнении должна быть строкой, числом, boolean или `null`; сравнение с другим полем парсер может разобрать, но интерпретатор его не исполняет. В `UPDATE SET` копирование значения другого поля поддерживается.

`UPDATE`/`DELETE` без `WHERE` применяются ко всем записям. `COPY` поддерживает `.csv` и `.json` и использует classpath-ресурсы, см. [жизненный цикл миграций](migrations.md).

## Значения

Литералы: `'строка'`, число, `true`, `false`, `null`, `ARRAY['a', 'b']`, `'{}'::json`. Элементы непустого массива должны иметь одинаковый тип; не предполагай поддержку произвольных SQL-выражений, функций, вложенных массивов или JSON-массивов верхнего уровня.

Для `date`/`timestamp` в миграциях используй строковые значения, соответствующие модели данных. Суффиксы `::date`/`::timestamp` из языка **фильтров** не являются корректным способом указать дату в миграциях: общий синтаксис `::identifier` здесь разбирается как `JsonValue`.

В `buildDataItemMap` литералы `null` пропускаются при построении карты. Для очистки существующего значения явно проверь поведение операции/сервиса на нужной версии: не считай `UPDATE ... SET field = null` гарантированным аналогом SQL-присваивания `NULL`.

## Источники

`dicts/src/main/java/com/backstage/app/dict/service/ddl/SqlParser.java`, `Interpreter.java`, `ast/ColumnType.java`, `ast/expression/table/operation/`.

Проверки: `SqlParserTest`, `CommonInterpreterTest`; ресурсы `dicts/src/test/resources/db/migration/dicts/`, включая типы, enum, размеры/default и импорт JSON/CSV.
