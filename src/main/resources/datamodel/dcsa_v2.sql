-- A script to initialize the tables relevant for the DCSA TNT interface v2.0

DROP TYPE IF EXISTS dcsa_v2_0.CarrierCodeListProvider CASCADE;
CREATE TYPE dcsa_v2_0.CarrierCodeListProvider AS ENUM (
    'SMDG',
    'NMFTA'
);

DROP TYPE IF EXISTS dcsa_v2_0.EmptyIndicatorCode CASCADE;
CREATE TYPE dcsa_v2_0.EmptyIndicatorCode AS ENUM (
    'EMPTY',
    'LADEN'
);

DROP TYPE IF EXISTS dcsa_v2_0.EventClassifierCode CASCADE;
CREATE TYPE dcsa_v2_0.EventClassifierCode AS ENUM (
    'PLN',
    'ACT',
    'EST'
);

DROP TYPE IF EXISTS dcsa_v2_0.EventType CASCADE;
CREATE TYPE dcsa_v2_0.EventType AS ENUM (
    'TRANSPORT',
    'SHIPMENT',
    'EQUIPMENT',
    'TRANSPORTEQUIPMENT'
);

DROP TYPE IF EXISTS dcsa_v2_0.EventTypeCode CASCADE;
CREATE TYPE dcsa_v2_0.EventTypeCode AS ENUM (
    'ARRI',
    'DEPA'
);

DROP TABLE IF EXISTS dcsa_v2_0.equipment_event CASCADE;
CREATE TABLE dcsa_v2_0.equipment_event (
    id uuid NOT NULL,
    event_type dcsa_v2_0.EventType NOT NULL,
    event_date_time timestamp with time zone NOT NULL,
    event_classifier_code dcsa_v2_0.EventClassifierCode NOT NULL,
    equipment_reference character varying(15),
    facility_type_code character varying(4) NOT NULL,
    un_location_code character varying(5) NOT NULL,
    facility_code character varying(11) NOT NULL,
    other_facility character varying(50),
    empty_indicator_code dcsa_v2_0.EmptyIndicatorCode NOT NULL,
    event_type_code dcsa_v2_0.EventTypeCode NOT NULL,
    transport_call_id uuid NOT NULL
);

DROP TABLE IF EXISTS dcsa_v2_0.schedule CASCADE;
CREATE TABLE dcsa_v2_0.schedule (
    id uuid NOT NULL,
    vessel_operator_carrier_code character varying(10) NOT NULL,
    vessel_partner_carrier_code character varying(10),
    start_date date,
    date_range interval,
    vessel_operator_carrier_code_list_provider dcsa_v2_0.CarrierCodeListProvider NOT NULL,
    vessel_partner_carrier_code_list_provider dcsa_v2_0.CarrierCodeListProvider
);

DROP TABLE IF EXISTS dcsa_v2_0.shipment_event CASCADE;
CREATE TABLE dcsa_v2_0.shipment_event (
    id uuid NOT NULL,
    event_type dcsa_v2_0.EventType NOT NULL,
    event_date_time timestamp with time zone NOT NULL,
    event_classifier_code dcsa_v2_0.EventClassifierCode NOT NULL,
    shipment_information_type_code character varying(3) NOT NULL,
    event_type_code dcsa_v2_0.EventTypeCode NOT NULL,
    transport_call_id uuid NOT NULL
);

DROP TABLE IF EXISTS dcsa_v2_0.transport_call CASCADE;
CREATE TABLE dcsa_v2_0.transport_call (
    id uuid NOT NULL,
    schedule_id uuid NOT NULL,
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

DROP TABLE IF EXISTS dcsa_v2_0.transport_equipment_event CASCADE;
CREATE TABLE dcsa_v2_0.transport_equipment_event (
    id uuid NOT NULL,
    event_type dcsa_v2_0.EventType NOT NULL,
    event_date_time timestamp with time zone NOT NULL,
    event_classifier_code dcsa_v2_0.EventClassifierCode NOT NULL,
    transport_reference character varying(50) NOT NULL,
    transport_leg_reference character varying NOT NULL,
    equipment_reference character varying(15),
    facility_type_code character varying(4) NOT NULL,
    un_location_code character varying(5) NOT NULL,
    facility_code character varying(11) NOT NULL,
    other_facility character varying(50),
    empty_indicator_code dcsa_v2_0.EmptyIndicatorCode NOT NULL,
    mode_of_transport_code integer,
    event_type_code dcsa_v2_0.EventTypeCode NOT NULL
);

DROP TABLE IF EXISTS dcsa_v2_0.transport_event CASCADE;
CREATE TABLE dcsa_v2_0.transport_event (
    id uuid NOT NULL,
    event_type dcsa_v2_0.EventType NOT NULL,
    event_date_time timestamp with time zone NOT NULL,
    transport_call_id uuid NOT NULL,
    delay_reason_code character varying(3),
    vessel_schedule_change_remark character varying(250),
    event_classifier_code dcsa_v2_0.EventClassifierCode NOT NULL,
    event_type_code dcsa_v2_0.EventTypeCode NOT NULL
);

ALTER TABLE ONLY dcsa_v2_0.equipment_event
    ADD CONSTRAINT equipment_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY dcsa_v2_0.schedule
    ADD CONSTRAINT schedule_pkey PRIMARY KEY (id);

ALTER TABLE ONLY dcsa_v2_0.shipment_event
    ADD CONSTRAINT shipment_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY dcsa_v2_0.transport_call
    ADD CONSTRAINT transport_call_pkey PRIMARY KEY (id);

ALTER TABLE ONLY dcsa_v2_0.transport_equipment_event
    ADD CONSTRAINT transport_equipment_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY dcsa_v2_0.transport_event
    ADD CONSTRAINT transport_event_pkey PRIMARY KEY (id);

CREATE INDEX equipment_event_to_transport_call_fk ON dcsa_v2_0.equipment_event USING btree (transport_call_id);

CREATE INDEX fki_shipment_event_transport_call_id_fkey ON dcsa_v2_0.shipment_event USING btree (transport_call_id);

CREATE INDEX schedule_fk ON dcsa_v2_0.transport_call USING btree (schedule_id);

CREATE INDEX transport_equipment_event_to_mode_of_transport ON dcsa_v2_0.transport_equipment_event USING btree (mode_of_transport_code);

CREATE INDEX transport_event_to_transport_call_fk ON dcsa_v2_0.transport_event USING btree (transport_call_id);

ALTER TABLE ONLY dcsa_v2_0.equipment_event
    ADD CONSTRAINT equipment_event_transport_call_id_fkey FOREIGN KEY (transport_call_id) REFERENCES dcsa_v2_0.transport_call(id);

ALTER TABLE ONLY dcsa_v2_0.shipment_event
    ADD CONSTRAINT shipment_event_transport_call_id_fkey FOREIGN KEY (transport_call_id) REFERENCES dcsa_v2_0.transport_call(id);

ALTER TABLE ONLY dcsa_v2_0.transport_call
    ADD CONSTRAINT transport_call_schedule_id_fkey FOREIGN KEY (schedule_id) REFERENCES dcsa_v2_0.schedule(id);

ALTER TABLE ONLY dcsa_v2_0.transport_event
    ADD CONSTRAINT transport_event_transport_call_id_fkey FOREIGN KEY (transport_call_id) REFERENCES dcsa_v2_0.transport_call(id);
