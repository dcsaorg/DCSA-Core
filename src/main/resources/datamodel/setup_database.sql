CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; -- Used to generate UUIDs
CREATE SCHEMA dcsa_v1_1;
CREATE SCHEMA dcsa_v2_0;
GRANT ALL PRIVILEGES ON DATABASE dcsa_openapi TO dcsa_db_owner;
GRANT ALL PRIVILEGES ON SCHEMA dcsa_v1_1 TO dcsa_db_owner;
GRANT ALL PRIVILEGES ON SCHEMA dcsa_v2_0 TO dcsa_db_owner;