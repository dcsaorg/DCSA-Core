-- A script to initialize the tables relevant for the DCSA TNT interface v1.2

DROP TYPE IF EXISTS dcsa_v1_1.empty_indicator_code CASCADE;
CREATE TYPE dcsa_v1_1.empty_indicator_code AS ENUM (
    'EMPTY',
    'LADEN'
);

DROP TYPE IF EXISTS dcsa_v1_1.event_classifier_code CASCADE;
CREATE TYPE dcsa_v1_1.event_classifier_code AS ENUM (
    'PLN',
    'ACT',
    'EST'
);

DROP TYPE IF EXISTS dcsa_v1_1.event_type CASCADE;
CREATE TYPE dcsa_v1_1.event_type AS ENUM (
    'EQUIPMENT',
    'TRANSPORT',
    'SHIPMENT',
    'TRANSPORTEQUIPMENT'
);

DROP TYPE IF EXISTS dcsa_v1_1.mode_of_transport_code CASCADE;
CREATE TYPE dcsa_v1_1.mode_of_transport_code AS ENUM (
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

DROP TABLE IF EXISTS dcsa_v1_1.event CASCADE;
CREATE TABLE dcsa_v1_1.event (
    event_classifier_code dcsa_v1_1.event_classifier_code NOT NULL,
    event_type text NOT NULL,
    event_date_time date NOT NULL,
    event_id uuid DEFAULT uuid_generate_v4(),
    event_type_code text NOT NULL
);

DROP TABLE IF EXISTS dcsa_v1_1.equipment_event CASCADE;
CREATE TABLE dcsa_v1_1.equipment_event (
    equipment_reference text NOT NULL,
    facility_type_code text NOT NULL,
    un_location_code text NOT NULL,
    facility_code text NOT NULL,
    other_facility text NOT NULL,
    empty_indicator_code text NOT NULL
)
INHERITS (dcsa_v1_1.event);


DROP TABLE IF EXISTS dcsa_v1_1.shipment_event CASCADE;
CREATE TABLE dcsa_v1_1.shipment_event (
    shipment_information_type_code text
)
INHERITS (dcsa_v1_1.event);


DROP TABLE IF EXISTS dcsa_v1_1.transport_event CASCADE;
CREATE TABLE dcsa_v1_1.transport_event (
    transport_reference text,
    transport_leg_reference text,
    facility_type_code text,
    un_location_code text,
    facility_code text,
    other_facility text,
    mode_of_transport_code text)
INHERITS (dcsa_v1_1.event);

-- We don't inherit from transport and equipment tables here,
-- to avoid receiving transport-equipment events when selecting for transport OR equipment events.
DROP TABLE IF EXISTS dcsa_v1_1.transport_equipment_event CASCADE;
CREATE TABLE dcsa_v1_1.transport_equipment_event (
    equipment_reference text NOT NULL,
    facility_type_code text NOT NULL,
    un_location_code text NOT NULL,
    facility_code text NOT NULL,
    other_facility text NOT NULL,
    empty_indicator_code text NOT NULL,
    transport_reference text NOT NULL,
    transport_leg_reference text NOT NULL,
    mode_of_transport_code text NOT NULL)
INHERITS (dcsa_v1_1.event);


