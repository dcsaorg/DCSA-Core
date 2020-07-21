const { Client } = require('pg');
const fs = require('fs');

const pgclient = new Client({
    host: process.env.POSTGRES_HOST,
    port: process.env.POSTGRES_PORT,
    user: 'postgres',
    password: 'postgres',
    database: 'postgres'
});

pgclient.connect();

const createdb =   fs.readFileSync('src/main/resources/datamodel/create_database.sql').toString();
console.log(createdb);

pgclient.query('CREATE DATABASE dcsa_openapi', (err, res) => {
    if (err) throw err
});
pgclient.end()


const setupdb =   fs.readFileSync('src/main/resources/datamodel/setup_database.sql').toString();
const dcsa_tnt_v1 =  fs.readFileSync('src/main/resources/datamodel/dcsa_tnt_v1.sql').toString();
const dcsa_v2 =  fs.readFileSync('src/main/resources/datamodel/dcsa_v2.sql').toString();
const data =  fs.readFileSync('src/main/resources/datamodel/test_data.sql').toString();

const dcsaclient = new Client({
    host: process.env.POSTGRES_HOST,
    port: process.env.POSTGRES_PORT,
    user: 'postgres',
    password: 'postgres',
    database: 'dcsa_openapi'
});


dcsaclient.query(setupdb, (err, res) => {
    if (err) throw err
});

dcsaclient.query(dcsa_tnt_v1, (err, res) => {
    if (err) throw err
});

dcsaclient.query(dcsa_v2, (err, res) => {
    if (err) throw err
});
dcsaclient.query(data, (err, res) => {
    if (err) throw err
});


dcsaclient.query('SELECT * FROM dcsa_v1_1.events', (err, res) => {
    if (err) throw err
    console.log(err, res.rows) // Print data
    dcsaclient.end()
});