/*
 *    Copyright 2019-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

-- Функция для выполнения запросов для версии '7.24.0'
create function upgrade_camunda_to_7_24_0()
	returns void as $$
begin
	-- Источник camunda-engine-7.24.0.jar!/org/camunda/bpm/engine/db/liquibase/upgrade/postgres_engine_7.23_to_7.24.sql
	insert into ACT_GE_SCHEMA_LOG values ('1300', CURRENT_TIMESTAMP, '7.24.0');
end;
$$ language plpgsql;

do
$$
	declare
		camunda_schema_log_table_exists boolean;
		camunda_version                 varchar;
	begin
		-- проверяем существование таблицы и сохраняем результат в переменную camunda_schema_log_table_exists
		select exists (select 1
		               from information_schema.tables
		               where table_schema = 'public'
			             and table_name = 'act_ge_schema_log')
		into camunda_schema_log_table_exists;

		-- если таблица существует, присваиваем значение переменной camunda_version
		if camunda_schema_log_table_exists then
			select version_
			into camunda_version
			from act_ge_schema_log
			order by timestamp_ desc, version_ desc
			limit 1;

			case
				when camunda_version = '7.23.0' then perform upgrade_camunda_to_7_24_0();
				else
				end case;
		end if;
	end
$$;

drop function upgrade_camunda_to_7_24_0();
