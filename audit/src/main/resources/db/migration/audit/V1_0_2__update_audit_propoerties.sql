alter table audit rename column properties to properties2;
alter table audit add column properties jsonb;

update audit set properties = (
select
	json_build_object(
			'properties',
			jsonb_set(
					properties2,
					'{properties}',
					(select jsonb_object_agg(
                               elem ->> 'key',
                               elem ->> 'value')
                    from jsonb_array_elements(properties2 -> 'properties') elem
					where properties2 ->> 'properties' is not null and elem ->> 'key' is not null
                    )
            ) -> 'properties',
			'fields',
			jsonb_set(
					properties2,
					'{fields}',
					(select  jsonb_object_agg(
                               	elem ->> 'name',
								json_build_object(
									'oldValue',
									elem ->> 'oldValue',
									'newValue',
									elem ->> 'newValue'))
                    from jsonb_array_elements(properties2 -> 'fields') elem
					where properties2 ->> 'fields' is not null and elem ->> 'name' is not null
                    )
            ) -> 'fields'
	)
);
