const { Client } = require('pg');
const fs = require('fs');

const createdb =   fs.readFileSync('src/main/resources/datamodel/create_database.sql').toString();
const setupdb =   fs.readFileSync('src/main/resources/datamodel/setup_database.sql').toString();
const dcsa_tnt_v1 =  fs.readFileSync('src/main/resources/datamodel/dcsa_tnt_v1.sql').toString();
const dcsa_v2 =  fs.readFileSync('src/main/resources/datamodel/dcsa_v2.sql').toString();
const data =  fs.readFileSync('src/main/resources/datamodel/test_data.sql').toString();


const pgclient = new Client({
    host: process.env.POSTGRES_HOST,
    port: process.env.POSTGRES_PORT,
    user: 'postgres',
    password: 'postgres',
    database: 'postgres'
});

pgclient.connect();


pgclient.query(setupdb, (err, res) => {
    if (err) throw err
});

pgclient.query(dcsa_tnt_v1, (err, res) => {
    if (err) throw err
});

pgclient.query(dcsa_v2, (err, res) => {
    if (err) throw err
});
pgclient.query(data, (err, res) => {
    if (err) throw err
});


pgclient.query('SELECT * FROM dcsa_v1_1.events', (err, res) => {
    if (err) throw err
    console.log(err, res.rows) // Print data
    dcsaclient.end()
    
});