--
-- PostgreSQL database dump
--

-- Dumped from database version 12.3
-- Dumped by pg_dump version 12.3

-- Started on 2020-07-08 15:04:35

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP DATABASE dcsa_tnt_v1;
--
-- TOC entry 2922 (class 1262 OID 24584)
-- Name: dcsa_tnt_v1; Type: DATABASE; Schema: -; Owner: -
--

CREATE DATABASE dcsa_tnt_v1 WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'English_United States.1252' LC_CTYPE = 'English_United States.1252';


connect dcsa_tnt_v1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 6 (class 2615 OID 24585)
-- Name: dcsa_v1.2; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "dcsa_v1.2";


--
-- TOC entry 5 (class 2615 OID 16464)
-- Name: dcsa v2.0; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "dcsa v2.0";


--
-- TOC entry 691 (class 1247 OID 24677)
-- Name: empty_indicator_code; Type: TYPE; Schema: dcsa_v1.2; Owner: -
--

CREATE TYPE "dcsa_v1.2".empty_indicator_code AS ENUM (
    'EMPTY',
    'LADEN'
);


--
-- TOC entry 673 (class 1247 OID 24640)
-- Name: event_classifier_code; Type: TYPE; Schema: dcsa_v1.2; Owner: -
--

CREATE TYPE "dcsa_v1.2".event_classifier_code AS ENUM (
    'PLN',
    'ACT',
    'EST'
);


--
-- TOC entry 676 (class 1247 OID 24648)
-- Name: event_type; Type: TYPE; Schema: dcsa_v1.2; Owner: -
--

CREATE TYPE "dcsa_v1.2".event_type AS ENUM (
    'EQUIPMENT',
    'TRANSPORT',
    'SHIPMENT',
    'TRANSPORTEQUIPMENT'
);


--
-- TOC entry 670 (class 1247 OID 24619)
-- Name: mode_of_transport_code; Type: TYPE; Schema: dcsa_v1.2; Owner: -
--

CREATE TYPE "dcsa_v1.2".mode_of_transport_code AS ENUM (
    '0',
    '1',
    '2',
    '3',
    '4',
    '5',
    '6',
    '7',
    '8',
    '9'
);


--
-- TOC entry 632 (class 1247 OID 16466)
-- Name: CarrierCodeListProvider; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."CarrierCodeListProvider" AS ENUM (
    'SMDG',
    'NMFTA'
);


--
-- TOC entry 635 (class 1247 OID 16472)
-- Name: EmptyIndicatorCode; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."EmptyIndicatorCode" AS ENUM (
    'EMPTY',
    'LADEN'
);


--
-- TOC entry 638 (class 1247 OID 16478)
-- Name: EventClassifierCode; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."EventClassifierCode" AS ENUM (
    'PLN',
    'ACT',
    'EST'
);


--
-- TOC entry 641 (class 1247 OID 16486)
-- Name: EventType; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."EventType" AS ENUM (
    'TRANSPORT',
    'SHIPMENT',
    'EQUIPMENT',
    'TRANSPORTEQUIPMENT'
);


--
-- TOC entry 644 (class 1247 OID 16496)
-- Name: EventTypeCode; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."EventTypeCode" AS ENUM (
    'ARRI',
    'DEPA'
);


SET default_table_access_method = heap;

--
-- TOC entry 209 (class 1259 OID 24586)
-- Name: event; Type: TABLE; Schema: dcsa_v1.2; Owner: -
--

CREATE TABLE "dcsa_v1.2".event (
    event_classifier_code text NOT NULL,
    event_type text NOT NULL,
    event_date_time date NOT NULL,
    event_id text NOT NULL,
    event_type_code text NOT NULL
);


--
-- TOC entry 212 (class 1259 OID 24670)
-- Name: equipment_event; Type: TABLE; Schema: dcsa_v1.2; Owner: -
--

CREATE TABLE "dcsa_v1.2".equipment_event (
    equipment_reference text NOT NULL,
    facility_type_code text NOT NULL,
    un_location_code text NOT NULL,
    facility_code text NOT NULL,
    other_facility text NOT NULL,
    empty_indicator_code text NOT NULL
)
INHERITS ("dcsa_v1.2".event);


--
-- TOC entry 210 (class 1259 OID 24657)
-- Name: shipment_event; Type: TABLE; Schema: dcsa_v1.2; Owner: -
--

CREATE TABLE "dcsa_v1.2".shipment_event (
    shipment_information_type_code text
)
INHERITS ("dcsa_v1.2".event);


--
-- TOC entry 211 (class 1259 OID 24664)
-- Name: transport_event; Type: TABLE; Schema: dcsa_v1.2; Owner: -
--

CREATE TABLE "dcsa_v1.2".transport_event (
    transport_reference text,
    transport_leg_reference text,
    facility_type_code text,
    un_location_code text,
    facility_code text,
    other_facility text,
    mode_of_transport_code text)
INHERITS ("dcsa_v1.2".event);


--
-- TOC entry 213 (class 1259 OID 24681)
-- Name: transport_equipment_event; Type: TABLE; Schema: dcsa_v1.2; Owner: -
--

CREATE TABLE "dcsa_v1.2".transport_equipment_event (
)
INHERITS ("dcsa_v1.2".equipment_event, "dcsa_v1.2".transport_event, "dcsa_v1.2".event);


--
-- TOC entry 203 (class 1259 OID 16504)
-- Name: mode_of_transport; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".mode_of_transport (
    code integer NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(250) NOT NULL,
    "DCSA_transport_type" character varying(50)
);


--
-- TOC entry 204 (class 1259 OID 16507)
-- Name: old_transport; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".old_transport (
    id integer NOT NULL,
    reference character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    mode_of_transport integer NOT NULL
);


--
-- TOC entry 205 (class 1259 OID 16510)
-- Name: old_vessel; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".old_vessel (
    imo_number character varying(7) NOT NULL,
    name character varying(35) NOT NULL,
    flag character varying(2) NOT NULL,
    call_sign_number character varying(50) NOT NULL,
    operator_carrier_code character varying(10) NOT NULL,
    transport_id integer NOT NULL
);


--
-- TOC entry 206 (class 1259 OID 16516)
-- Name: shipment_event; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".shipment_event (
    id integer NOT NULL,
    event_type "dcsa v2.0"."EventType" NOT NULL,
    event_date_time timestamp with time zone NOT NULL,
    event_classifier_code "dcsa v2.0"."EventClassifierCode" NOT NULL,
    shipment_information_type_code character varying(3) NOT NULL,
    event_type_code "dcsa v2.0"."EventTypeCode" NOT NULL,
    transport_call_id integer NOT NULL
);


--
-- TOC entry 207 (class 1259 OID 16525)
-- Name: transport_equipment_event; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".transport_equipment_event (
    id integer NOT NULL,
    event_type "dcsa v2.0"."EventType" NOT NULL,
    event_date_time timestamp with time zone NOT NULL,
    event_classifier_code "dcsa v2.0"."EventClassifierCode" NOT NULL,
    transport_reference character varying(50) NOT NULL,
    transport_leg_reference character varying NOT NULL,
    equipment_reference character varying(15),
    facility_type_code character varying(4) NOT NULL,
    un_location_code character varying(5) NOT NULL,
    facility_code character varying(11) NOT NULL,
    other_facility character varying(50),
    empty_indicator_code "dcsa v2.0"."EmptyIndicatorCode" NOT NULL,
    mode_of_transport_code integer,
    event_type_code "dcsa v2.0"."EventTypeCode" NOT NULL
);


--
-- TOC entry 208 (class 1259 OID 16531)
-- Name: transport_event; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".transport_event (
    id integer NOT NULL,
    event_type "dcsa v2.0"."EventType" NOT NULL,
    event_date_time timestamp with time zone NOT NULL,
    transport_call_id integer NOT NULL,
    delay_reason_code character varying(3),
    vessel_schedule_change_remark character varying(250),
    event_classifier_code "dcsa v2.0"."EventClassifierCode" NOT NULL,
    event_type_code "dcsa v2.0"."EventTypeCode" NOT NULL
);


--
-- TOC entry 2760 (class 2606 OID 16537)
-- Name: mode_of_transport mode_of_transport_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".mode_of_transport
    ADD CONSTRAINT mode_of_transport_pkey PRIMARY KEY (code);


--
-- TOC entry 2769 (class 2606 OID 16541)
-- Name: shipment_event shipment_event_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".shipment_event
    ADD CONSTRAINT shipment_event_pkey PRIMARY KEY (id);


--
-- TOC entry 2771 (class 2606 OID 16545)
-- Name: transport_equipment_event transport_equipment_event_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_equipment_event
    ADD CONSTRAINT transport_equipment_event_pkey PRIMARY KEY (id);


--
-- TOC entry 2774 (class 2606 OID 16547)
-- Name: transport_event transport_event_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_event
    ADD CONSTRAINT transport_event_pkey PRIMARY KEY (id);


--
-- TOC entry 2763 (class 2606 OID 16549)
-- Name: old_transport transport_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".old_transport
    ADD CONSTRAINT transport_pkey PRIMARY KEY (id);


--
-- TOC entry 2766 (class 2606 OID 16551)
-- Name: old_vessel vessel_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".old_vessel
    ADD CONSTRAINT vessel_pkey PRIMARY KEY (imo_number);


--
-- TOC entry 2767 (class 1259 OID 24581)
-- Name: fki_shipment_event_transport_call_id_fkey; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX fki_shipment_event_transport_call_id_fkey ON "dcsa v2.0".shipment_event USING btree (transport_call_id);


--
-- TOC entry 2761 (class 1259 OID 16553)
-- Name: mode_of_transport_fk; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX mode_of_transport_fk ON "dcsa v2.0".old_transport USING btree (mode_of_transport);


--
-- TOC entry 2772 (class 1259 OID 16555)
-- Name: transport_equipment_event_to_mode_of_transport; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX transport_equipment_event_to_mode_of_transport ON "dcsa v2.0".transport_equipment_event USING btree (mode_of_transport_code);


--
-- TOC entry 2775 (class 1259 OID 16556)
-- Name: transport_event_to_transport_call_fk; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX transport_event_to_transport_call_fk ON "dcsa v2.0".transport_event USING btree (transport_call_id);


--
-- TOC entry 2764 (class 1259 OID 16557)
-- Name: transport_id_fk; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX transport_id_fk ON "dcsa v2.0".old_vessel USING btree (transport_id);


--
-- TOC entry 2778 (class 2606 OID 16568)
-- Name: transport_equipment_event transport_equipment_event_mode_of_transport_code_fkey; Type: FK CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_equipment_event
    ADD CONSTRAINT transport_equipment_event_mode_of_transport_code_fkey FOREIGN KEY (mode_of_transport_code) REFERENCES "dcsa v2.0".mode_of_transport(code);


--
-- TOC entry 2779 (class 2606 OID 16573)
-- Name: transport_equipment_event transport_equipment_event_mode_of_transport_code_fkey1; Type: FK CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_equipment_event
    ADD CONSTRAINT transport_equipment_event_mode_of_transport_code_fkey1 FOREIGN KEY (mode_of_transport_code) REFERENCES "dcsa v2.0".mode_of_transport(code);


--
-- TOC entry 2776 (class 2606 OID 16583)
-- Name: old_transport transport_mode_of_transport_fkey; Type: FK CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".old_transport
    ADD CONSTRAINT transport_mode_of_transport_fkey FOREIGN KEY (mode_of_transport) REFERENCES "dcsa v2.0".mode_of_transport(code);


--
-- TOC entry 2777 (class 2606 OID 16588)
-- Name: old_vessel vessel_transport_id_fkey; Type: FK CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".old_vessel
    ADD CONSTRAINT vessel_transport_id_fkey FOREIGN KEY (transport_id) REFERENCES "dcsa v2.0".old_transport(id);


-- Completed on 2020-07-08 15:04:35

--
-- PostgreSQL database dump complete
--

