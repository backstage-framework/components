-- Функция для выполнения запросов для версии '7.23.0'
create function upgrade_camunda_to_7_23_0()
	returns void as $$
begin
	-- Источник camunda-engine-7.23.0.jar!/org/camunda/bpm/engine/db/liquibase/upgrade/postgres_engine_7.22_to_7.23.sql
	insert into ACT_GE_SCHEMA_LOG values ('1200', CURRENT_TIMESTAMP, '7.23.0');

	alter table ACT_HI_COMMENT add column REV_ integer not null default 1;
	alter table ACT_RU_EXECUTION add column PROC_DEF_KEY_ varchar(255);
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
				when camunda_version = '7.22.0' then perform upgrade_camunda_to_7_23_0();
				else
			end case;
		end if;
	end
$$;

drop function upgrade_camunda_to_7_23_0();
