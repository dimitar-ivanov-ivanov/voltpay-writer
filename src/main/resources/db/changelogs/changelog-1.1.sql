-- partition the tables using partman
CREATE SCHEMA IF NOT EXISTS partman;

CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA partman;  -- required
CREATE EXTENSION IF NOT EXISTS pg_partman SCHEMA partman;

SELECT partman.create_parent(
  p_parent_table := 'write.payment_core',
  p_control := 'created_at',
  p_type := 'time',
  p_interval := 'monthly',
  p_premake := 2
);

SELECT partman.create_parent(
  p_parent_table := 'write.payment_metadata',
  p_control := 'created_at',
  p_type := 'time',
  p_interval := 'monthly',
  p_premake := 2
);

SELECT partman.create_parent(
  p_parent_table := 'write.payment_notes',
  p_control := 'created_at',
  p_type := 'time',
  p_interval := 'monthly',
  p_premake := 2
);

