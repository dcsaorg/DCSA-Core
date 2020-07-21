const { Client } = require('pg');
const fs = require('fs');

const pgclient = new Client({
    host: process.env.POSTGRES_HOST,
    port: process.env.POSTGRES_PORT,
    user: 'dcsa_db_owner',
    password: 'postgres',
    database: 'dcsa_openapi'
});

pgclient.connect();

const createdb =   fs.readFileSync('src/main/resources/datamodel/setup_databases.sql').toString();
const dcsa_tnt_v1 =  fs.readFileSync('src/main/resources/datamodel/dcsa_tnt_v1.sql').toString();
const dcsa_v2 =  fs.readFileSync('src/main/resources/datamodel/dcsa_v2.sql').toString();
const data =  fs.readFileSync('src/main/resources/datamodel/test_data.sql').toString();

pgclient.query(createdb, (err, res) => {
    if (err) throw err
});

pgclient.query(dcsa_tnt_v1, (err, res) => {
    if (err) throw err
});

pgclient.query(dcsa_v2, (err, res) => {
    if (err) throw err
});
pgclient.query(test_data, (err, res) => {
    if (err) throw err
});


pgclient.query('SELECT * FROM dcsa_v1_1.events', (err, res) => {
    if (err) throw err
    console.log(err, res.rows) // Print data
    pgclient.end()
});