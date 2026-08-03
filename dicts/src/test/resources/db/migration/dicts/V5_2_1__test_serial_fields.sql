create table testSerialFields (name text, counter serial);

insert into testSerialFields (name) values ('a');
insert into testSerialFields (name) values ('b');
insert into testSerialFields (name) values ('c');

alter table testSerialFields alter column counter restart with 1;

insert into testSerialFields (name) values ('d');
insert into testSerialFields (name) values ('e');
insert into testSerialFields (name) values ('f');
