CREATE SCHEMA IF NOT EXISTS write;

 CREATE TABLE IF NOT EXISTS write.payment_core (
             id VARCHAR(30),
             amount NUMERIC(20, 6),
             status INTEGER,
             currency VARCHAR(3),
             cust_id BIGINT,
             type VARCHAR(3),
             created_at TIMESTAMP NOT NULL,
             PRIMARY KEY (id, created_at)
 );

 CREATE TABLE IF NOT EXISTS write.payment_metadata (
             id VARCHAR(30) not null,
             created_at TIMESTAMP NOT NULL,
             updated_at TIMESTAMP,
             version INTEGER,
             PRIMARY KEY (id, created_at)
 );

 CREATE TABLE IF NOT EXISTS write.payment_notes (
               id VARCHAR(30) NOT NULL,
               external_ref_id VARCHAR(200),
               comment VARCHAR(300),
               created_at TIMESTAMP NOT NULL,
               PRIMARY KEY (id, created_at)
 );

 CREATE TABLE IF NOT EXISTS write.idempotency (
     id VARCHAR(255) PRIMARY KEY,
     date DATE
 );