--
-- PostgreSQL database dump
--

-- Dumped from database version 12.3
-- Dumped by pg_dump version 12.3

-- Started on 2020-07-08 15:03:00

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

DROP DATABASE dcsa_tnt_v2;
--
-- TOC entry 2880 (class 1262 OID 16393)
-- Name: dcsa_tnt_v2; Type: DATABASE; Schema: -; Owner: -
--

CREATE DATABASE dcsa_tnt_v2 WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'English_United States.1252' LC_CTYPE = 'English_United States.1252';


\connect dcsa_tnt_v2

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
-- TOC entry 5 (class 2615 OID 16464)
-- Name: dcsa v2.0; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA "dcsa v2.0";


--
-- TOC entry 626 (class 1247 OID 16466)
-- Name: CarrierCodeListProvider; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."CarrierCodeListProvider" AS ENUM (
    'SMDG',
    'NMFTA'
);


--
-- TOC entry 629 (class 1247 OID 16472)
-- Name: EmptyIndicatorCode; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."EmptyIndicatorCode" AS ENUM (
    'EMPTY',
    'LADEN'
);


--
-- TOC entry 632 (class 1247 OID 16478)
-- Name: EventClassifierCode; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."EventClassifierCode" AS ENUM (
    'PLN',
    'ACT',
    'EST'
);


--
-- TOC entry 635 (class 1247 OID 16486)
-- Name: EventType; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."EventType" AS ENUM (
    'TRANSPORT',
    'SHIPMENT',
    'EQUIPMENT',
    'TRANSPORTEQUIPMENT'
);


--
-- TOC entry 638 (class 1247 OID 16496)
-- Name: EventTypeCode; Type: TYPE; Schema: dcsa v2.0; Owner: -
--

CREATE TYPE "dcsa v2.0"."EventTypeCode" AS ENUM (
    'ARRI',
    'DEPA'
);


SET default_table_access_method = heap;

--
-- TOC entry 202 (class 1259 OID 16501)
-- Name: equipment_event; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".equipment_event (
    id integer NOT NULL,
    event_type "dcsa v2.0"."EventType" NOT NULL,
    event_date_time timestamp with time zone NOT NULL,
    event_classifier_code "dcsa v2.0"."EventClassifierCode" NOT NULL,
    equipment_reference character varying(15),
    facility_type_code character varying(4) NOT NULL,
    un_location_code character varying(5) NOT NULL,
    facility_code character varying(11) NOT NULL,
    other_facility character varying(50),
    empty_indicator_code "dcsa v2.0"."EmptyIndicatorCode" NOT NULL,
    event_type_code "dcsa v2.0"."EventTypeCode" NOT NULL,
    transport_call_id integer NOT NULL
);


--
-- TOC entry 203 (class 1259 OID 16513)
-- Name: schedule; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".schedule (
    id integer NOT NULL,
    vessel_operator_carrier_code character varying(10) NOT NULL,
    vessel_partner_carrier_code character varying(10),
    start_date date,
    date_range interval,
    vessel_operator_carrier_code_list_provider "dcsa v2.0"."CarrierCodeListProvider" NOT NULL,
    vessel_partner_carrier_code_list_provider "dcsa v2.0"."CarrierCodeListProvider"
);


--
-- TOC entry 204 (class 1259 OID 16516)
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
-- TOC entry 205 (class 1259 OID 16519)
-- Name: transport_call; Type: TABLE; Schema: dcsa v2.0; Owner: -
--

CREATE TABLE "dcsa v2.0".transport_call (
    id integer NOT NULL,
    schedule_id integer NOT NULL,
    carrier_service_code character varying,
    vessel_imo_number character varying(7),
    vessel_name character varying(35),
    carrier_voyage_number character varying(50) NOT NULL,
    un_location_code character varying(5) NOT NULL,
    un_location_name character varying(70),
    transport_call_number integer,
    facility_type_code character varying(4) NOT NULL,
    facility_code character varying(11) NOT NULL,
    other_facility character varying(50)
);


--
-- TOC entry 206 (class 1259 OID 16525)
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
-- TOC entry 207 (class 1259 OID 16531)
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
-- TOC entry 2869 (class 0 OID 16501)
-- Dependencies: 202
-- Data for Name: equipment_event; Type: TABLE DATA; Schema: dcsa v2.0; Owner: -
--

COPY "dcsa v2.0".equipment_event (id, event_type, event_date_time, event_classifier_code, equipment_reference, facility_type_code, un_location_code, facility_code, other_facility, empty_indicator_code, event_type_code, transport_call_id) FROM stdin;
\.


--
-- TOC entry 2870 (class 0 OID 16513)
-- Dependencies: 203
-- Data for Name: schedule; Type: TABLE DATA; Schema: dcsa v2.0; Owner: -
--

COPY "dcsa v2.0".schedule (id, vessel_operator_carrier_code, vessel_partner_carrier_code, start_date, date_range, vessel_operator_carrier_code_list_provider, vessel_partner_carrier_code_list_provider) FROM stdin;
\.


--
-- TOC entry 2871 (class 0 OID 16516)
-- Dependencies: 204
-- Data for Name: shipment_event; Type: TABLE DATA; Schema: dcsa v2.0; Owner: -
--

COPY "dcsa v2.0".shipment_event (id, event_type, event_date_time, event_classifier_code, shipment_information_type_code, event_type_code, transport_call_id) FROM stdin;
\.


--
-- TOC entry 2872 (class 0 OID 16519)
-- Dependencies: 205
-- Data for Name: transport_call; Type: TABLE DATA; Schema: dcsa v2.0; Owner: -
--

COPY "dcsa v2.0".transport_call (id, schedule_id, carrier_service_code, vessel_imo_number, vessel_name, carrier_voyage_number, un_location_code, un_location_name, transport_call_number, facility_type_code, facility_code, other_facility) FROM stdin;
\.


--
-- TOC entry 2873 (class 0 OID 16525)
-- Dependencies: 206
-- Data for Name: transport_equipment_event; Type: TABLE DATA; Schema: dcsa v2.0; Owner: -
--

COPY "dcsa v2.0".transport_equipment_event (id, event_type, event_date_time, event_classifier_code, transport_reference, transport_leg_reference, equipment_reference, facility_type_code, un_location_code, facility_code, other_facility, empty_indicator_code, mode_of_transport_code, event_type_code) FROM stdin;
\.


--
-- TOC entry 2874 (class 0 OID 16531)
-- Dependencies: 207
-- Data for Name: transport_event; Type: TABLE DATA; Schema: dcsa v2.0; Owner: -
--

COPY "dcsa v2.0".transport_event (id, event_type, event_date_time, transport_call_id, delay_reason_code, vessel_schedule_change_remark, event_classifier_code, event_type_code) FROM stdin;
\.


--
-- TOC entry 2723 (class 2606 OID 16535)
-- Name: equipment_event equipment_event_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".equipment_event
    ADD CONSTRAINT equipment_event_pkey PRIMARY KEY (id);


--
-- TOC entry 2726 (class 2606 OID 16539)
-- Name: schedule schedule_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".schedule
    ADD CONSTRAINT schedule_pkey PRIMARY KEY (id);


--
-- TOC entry 2729 (class 2606 OID 16541)
-- Name: shipment_event shipment_event_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".shipment_event
    ADD CONSTRAINT shipment_event_pkey PRIMARY KEY (id);


--
-- TOC entry 2732 (class 2606 OID 16543)
-- Name: transport_call transport_call_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_call
    ADD CONSTRAINT transport_call_pkey PRIMARY KEY (id);


--
-- TOC entry 2734 (class 2606 OID 16545)
-- Name: transport_equipment_event transport_equipment_event_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_equipment_event
    ADD CONSTRAINT transport_equipment_event_pkey PRIMARY KEY (id);


--
-- TOC entry 2737 (class 2606 OID 16547)
-- Name: transport_event transport_event_pkey; Type: CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_event
    ADD CONSTRAINT transport_event_pkey PRIMARY KEY (id);


--
-- TOC entry 2724 (class 1259 OID 16552)
-- Name: equipment_event_to_transport_call_fk; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX equipment_event_to_transport_call_fk ON "dcsa v2.0".equipment_event USING btree (transport_call_id);


--
-- TOC entry 2727 (class 1259 OID 24581)
-- Name: fki_shipment_event_transport_call_id_fkey; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX fki_shipment_event_transport_call_id_fkey ON "dcsa v2.0".shipment_event USING btree (transport_call_id);


--
-- TOC entry 2730 (class 1259 OID 16554)
-- Name: schedule_fk; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX schedule_fk ON "dcsa v2.0".transport_call USING btree (schedule_id);


--
-- TOC entry 2735 (class 1259 OID 16555)
-- Name: transport_equipment_event_to_mode_of_transport; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX transport_equipment_event_to_mode_of_transport ON "dcsa v2.0".transport_equipment_event USING btree (mode_of_transport_code);


--
-- TOC entry 2738 (class 1259 OID 16556)
-- Name: transport_event_to_transport_call_fk; Type: INDEX; Schema: dcsa v2.0; Owner: -
--

CREATE INDEX transport_event_to_transport_call_fk ON "dcsa v2.0".transport_event USING btree (transport_call_id);


--
-- TOC entry 2739 (class 2606 OID 16558)
-- Name: equipment_event equipment_event_transport_call_id_fkey; Type: FK CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".equipment_event
    ADD CONSTRAINT equipment_event_transport_call_id_fkey FOREIGN KEY (transport_call_id) REFERENCES "dcsa v2.0".transport_call(id);


--
-- TOC entry 2740 (class 2606 OID 24576)
-- Name: shipment_event shipment_event_transport_call_id_fkey; Type: FK CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".shipment_event
    ADD CONSTRAINT shipment_event_transport_call_id_fkey FOREIGN KEY (transport_call_id) REFERENCES "dcsa v2.0".transport_call(id);


--
-- TOC entry 2741 (class 2606 OID 16563)
-- Name: transport_call transport_call_schedule_id_fkey; Type: FK CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_call
    ADD CONSTRAINT transport_call_schedule_id_fkey FOREIGN KEY (schedule_id) REFERENCES "dcsa v2.0".schedule(id);


--
-- TOC entry 2742 (class 2606 OID 16578)
-- Name: transport_event transport_event_transport_call_id_fkey; Type: FK CONSTRAINT; Schema: dcsa v2.0; Owner: -
--

ALTER TABLE ONLY "dcsa v2.0".transport_event
    ADD CONSTRAINT transport_event_transport_call_id_fkey FOREIGN KEY (transport_call_id) REFERENCES "dcsa v2.0".transport_call(id);


-- Completed on 2020-07-08 15:03:01

--
-- PostgreSQL database dump complete
--

