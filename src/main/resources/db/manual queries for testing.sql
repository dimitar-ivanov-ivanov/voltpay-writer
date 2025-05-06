create schema write;

CREATE SCHEMA IF NOT EXISTS partman;

CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA partman;

CREATE EXTENSION IF NOT EXISTS pg_partman SCHEMA partman;

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

select * from write.payment_core where id = '01JT2XQPGQ7EMEWJ633AXKN3S8-001'

select * from write.payment_metadata pm;

select * from write.payment_notes pt;

select count(*) from write.idempotency i ;

-- successful -> d27a6dc7-e679-49a0-8926-e82326f44b87
-- failed -> c5f10fb1-5797-4294-9d5f-d1787060594a, 974ff56a-9b4a-4b3d-b3dc-812dd8e63d7e 3e772b8b-2e21-4dac-89f0-55eb802fc0e8 426d9c45-8b1d-4a5a-9ad1-bb39937d54be
select * from write.idempotency i where id = '426d9c45-8b1d-4a5a-9ad1-bb39937d54be'

truncate write.payment_core;
truncate write.payment_metadata;
truncate write.payment_notes;
truncate write.idempotency;
