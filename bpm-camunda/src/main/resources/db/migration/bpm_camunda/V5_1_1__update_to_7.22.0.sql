-- Функция для выполнения запросов для версии '7.21.0'
create function upgrade_camunda_to_7_21_0()
	returns void as $$
begin
	-- Источник camunda-engine-7.22.0.jar!/org/camunda/bpm/engine/db/liquibase/upgrade/postgres_engine_7.20_to_7.21.sql
	insert into ACT_GE_SCHEMA_LOG
	values ('1000', CURRENT_TIMESTAMP, '7.21.0');
	alter table ACT_RU_EXT_TASK add column CREATE_TIME_ timestamp;
	alter table ACT_RU_JOB add column ROOT_PROC_INST_ID_ varchar(64);
	create index ACT_IDX_JOB_ROOT_PROCINST on ACT_RU_JOB(ROOT_PROC_INST_ID_);
end;
$$ language plpgsql;

-- Функция для выполнения запросов для версии '7.22.0'
create function upgrade_camunda_to_7_22_0()
	returns void as $$
begin
	-- Источник camunda-engine-7.22.0.jar!/org/camunda/bpm/engine/db/liquibase/upgrade/postgres_engine_7.21_to_7.22.sql
	insert into ACT_GE_SCHEMA_LOG values ('1100', CURRENT_TIMESTAMP, '7.22.0');
	alter table ACT_RU_TASK add column TASK_STATE_ varchar(64);
	alter table ACT_HI_TASKINST add column TASK_STATE_ varchar(64);
	alter table ACT_RU_JOB add column BATCH_ID_ varchar(64);
	alter table ACT_HI_JOB_LOG add column BATCH_ID_ varchar(64);
	alter table ACT_HI_PROCINST add RESTARTED_PROC_INST_ID_ varchar(64);
	create index ACT_IDX_HI_PRO_RST_PRO_INST_ID on ACT_HI_PROCINST(RESTARTED_PROC_INST_ID_);
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
				when camunda_version = '7.19.0' then
					-- Запросы для версии '7.20.0'
					-- Источник camunda-engine-7.22.0.jar!/org/camunda/bpm/engine/db/liquibase/upgrade/postgres_engine_7.19_to_7.20.sql
					insert into ACT_GE_SCHEMA_LOG values ('900', CURRENT_TIMESTAMP, '7.20.0');

					perform upgrade_camunda_to_7_21_0();
					perform upgrade_camunda_to_7_22_0();

				when camunda_version = '7.20.0' then
					perform upgrade_camunda_to_7_21_0();
					perform upgrade_camunda_to_7_22_0();

				when camunda_version = '7.21.0' then
					perform upgrade_camunda_to_7_22_0();
				else
			end case;
		end if;
	end
$$;

drop function upgrade_camunda_to_7_21_0();
drop function upgrade_camunda_to_7_22_0();
