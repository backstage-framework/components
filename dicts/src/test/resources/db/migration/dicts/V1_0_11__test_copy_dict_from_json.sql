create table importedFromJson (field1 int, field2 text[], field3 int[]);

copy importedFromJson from '/testMultivaluedColumnImport.json';
