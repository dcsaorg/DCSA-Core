-- A script to initialize the tables relevant for the DCSA TNT interface v1.2
\connect dcsa_openapi


DROP TABLE IF EXISTS dcsa_v1_1.event CASCADE;
CREATE TABLE dcsa_v1_1.event (
    event_classifier_code text NOT NULL,
    event_type text NOT NULL,
    event_date_time date NOT NULL,
    event_id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
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
    shipment_id uuid NOT NULL,
    shipment_information_type_code text
)
INHERITS (dcsa_v1_1.event);


DROP TABLE IF EXISTS dcsa_v1_1.transport_event CASCADE;
CREATE TABLE dcsa_v1_1.transport_event (
    transport_reference text NOT NULL,
    transport_leg_reference text NOT NULL,
    facility_type_code text NOT NULL,
    un_location_code text NOT NULL,
    facility_code text NOT NULL,
    other_facility text NOT NULL,
    mode_of_transport_code text NOT NULL)
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

DROP TABLE IF EXISTS dcsa_v1_1.event_subscription CASCADE;
CREATE TABLE dcsa_v1_1.event_subscription (
    subscription_id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    callback_url text NOT NULL,
    booking_reference text,
    event_type text, --This field must be able to contain multiple event types. Currently it does not.
    bill_of_lading_number text,
    equipment_reference text
    );

DROP TABLE IF EXISTS dcsa_v1_1.shipment CASCADE;
CREATE TABLE dcsa_v1_1.shipment (
    shipment_id uuid NOT NULL,
    booking_reference text, -- The identifier for a shipment, which is issued by and unique within each of the carriers.
    booking_datetime timestamp, -- The date and time of the booking request.
    blnumber varchar(20), -- Transport Document ID is an identifier that links to a shipment. Bill of lading is the legal document issued to the customer which confirms the carrier's receipt of the cargo from the customer acknowledging goods being shipped and specifying the terms of delivery.
    shipper_name varchar(50), -- The name of the shipper, who requested the booking
    consignee_name varchar(50), -- The name of the consignee
    collection_origin varchar(250), -- The location through which the shipment originates. It can be defined as a UN Location Code value or an address. The customer (shipper) needs to place a booking in order to ship the cargo (commodity) from an origin to destination. This attribute specifies the location of the origin.
    collection_dateTime timestamp, -- The date and the time that the shipment items need to be collected from the origin.
    delivery_destination varchar(250), -- The location to which the shipment is destined. It can be defined as a UN Location Code value or an address. The customer (shipper) needs to place a booking in order to ship the cargo (commodity) from an origin to destination. This attribute specifies the location of the destination. Also known as 'place of carrier delivery'.
    delivery_datetime timestamp , -- The date (and when possible time) that the shipment items need to be delivered to the destination.
    carrier_code varchar(10) -- The Carrier Code represents a concatenation of the Code List Provider Code and the Code List Provider. A hyphen is used between the two codes. The unique carrier identifier is sourced from either the NMFTA SCAC codes list or the SMDG Master Liner codes list.
    );
