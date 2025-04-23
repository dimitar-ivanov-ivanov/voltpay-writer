create schema write;

SELECT * FROM pg_available_extensions WHERE name = 'pg_partman';

SELECT tablename FROM pg_tables WHERE tablename LIKE '%payment%';

SELECT partman.create_parent(
  p_parent_table := 'write.payment_core',
  p_control := 'created_at',
  p_type := 'time',
  p_interval := 'monthly',
  p_premake := 2
);

SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'partman';

select * 
FROM pg_catalog.pg_partitioned_table p
JOIN pg_catalog.pg_class c ON p.partrelid = c.oid
JOIN pg_namespace n ON c.relnamespace = n.oid

-- Manual clean up of changelogs and partmant
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC;
select * from partman.part_config ;

truncate databasechangelog;

truncate partman.part_config CASCADE; -- cascade because config_sub has foreign key to part_config 
truncate partman.part_config_sub;
truncate partman.template_write_payment_core;
truncate partman.template_write_payment_metadata;
truncate partman.template_write_payment_notes;

drop table write.payment_core;
drop table write.payment_metadata;
drop table write.payment_notes;
drop table write.idempotency;

-------------------
select count(*) from write.payment_core pc where id = '01JSGH8Y62QAHP5FTW4S9S3FZ4-001';

select count(*) from write.payment_core

select * from write.payment_metadata pm;

select * from write.payment_notes pt;

select count(*) from write.idempotency i ;

truncate write.payment_core;
truncate write.payment_metadata;
truncate write.payment_notes;
truncate write.idempotency;
