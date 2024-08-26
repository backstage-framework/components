create table testDict1['testDict1'] (
    field1['field1'] text,
    field2['field2'] int,
    field3['field3'] decimal,
	field4['field4'] text not null,
	field5['field5'] text not null default 'defaultValue',
	field6['field6'] text default 'defaultValue'
);

alter table testDict1 alter column field1 set minSize = 1;
alter table testDict1 alter column field1 set maxSize = 30;
alter table testDict1 alter column field2 set minSize = 1;
alter table testDict1 alter column field2 set maxSize = 30;
alter table testDict1 alter column field3 set minSize = 1;
alter table testDict1 alter column field3 set maxSize = 30;

alter table testDict1 alter column field1 set default 'defaultValue';
alter table testDict1 alter column field2 set default 2;
alter table testDict1 alter column field3 set default 2;

insert into testDict1(field4) values('Тест');
