-- Database setup script
-- Needs to be executed by user postgres or equivalent

DROP DATABASE IF EXISTS dcsa_tnt_v1;

CREATE DATABASE dcsa_tnt_v1 ENCODING = 'UTF8' ;

\connect dcsa_tnt_v1

CREATE SCHEMA "dcsa_v1.2";

GRANT ALL PRIVILEGES ON DATABASE dcsa_tnt_v1 TO dcsa_db_owner;

GRANT ALL PRIVILEGES ON SCHEMA "dcsa_v1.2" TO dcsa_db_owner;

DROP DATABASE IF EXISTS dcsa_tnt_v2;

CREATE DATABASE dcsa_tnt_v2 ENCODING = 'UTF8';

\connect dcsa_tnt_v2

CREATE SCHEMA "dcsa_v2.0";

GRANT ALL PRIVILEGES ON DATABASE dcsa_tnt_v2 TO dcsa_db_owner;

GRANT ALL PRIVILEGES ON SCHEMA "dcsa_v2.0" TO dcsa_db_owner;
