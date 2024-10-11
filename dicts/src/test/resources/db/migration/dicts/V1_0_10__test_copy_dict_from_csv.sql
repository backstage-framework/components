create table importedFromCsv (field1 int, field2 text[], field3 int[]);

copy importedFromCsv from '/testMultivaluedColumnImport.csv';
