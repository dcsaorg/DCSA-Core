Setting up the database
=======================

The reference implementation uses PostgreSQL as the underlying database

Create the DCSA DB owner
------------------------
As user postgres, execute
```
CREATE USER dcsa_db_owner WITH PASSWORD '9c072fe8-c59c-11ea-b8d1-7b6577e9f3f5';
```
(remember to modify the password).

Create the DCSA databases
-------------------------
Execute set_up_databases.sql for instance with
```
cat set_up_databases.sql |sudo -u postgres psql
```

Populate the databases with tables
----------------------------------
```
cat dcsa_tnt_v1.sql | psql -U dcsa_db_owner dcsa_openapi
cat dcsa_v2.sql | psql -U dcsa_db_owner dcsa_openapi
cat populate_testdata.sql | psql -U dcsa_db_owner dcsa_openapi
```
