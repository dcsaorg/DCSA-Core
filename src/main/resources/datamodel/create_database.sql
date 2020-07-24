-- Database setup script
-- Needs to be executed by user postgres or equivalent

DROP DATABASE IF EXISTS dcsa_openapi;
CREATE DATABASE dcsa_openapi ENCODING = 'UTF8' ;
\connect dcsa_openapi
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; -- Used to generate UUIDs
CREATE SCHEMA dcsa_v1_1;
CREATE SCHEMA dcsa_v2_0;
CREATE USER dcsa_db_owner WITH PASSWORD '9c072fe8-c59c-11ea-b8d1-7b6577e9f3f5';
GRANT ALL PRIVILEGES ON DATABASE dcsa_openapi TO dcsa_db_owner;
GRANT ALL PRIVILEGES ON SCHEMA dcsa_v1_1 TO dcsa_db_owner;
GRANT ALL PRIVILEGES ON SCHEMA dcsa_v2_0 TO dcsa_db_owner;
ALTER DEFAULT PRIVILEGES IN SCHEMA dcsa_v1_1 GRANT ALL ON TABLES TO dcsa_db_owner;
ALTER DEFAULT PRIVILEGES IN SCHEMA dcsa_v2_0 GRANT ALL ON TABLES TO dcsa_db_owner;
