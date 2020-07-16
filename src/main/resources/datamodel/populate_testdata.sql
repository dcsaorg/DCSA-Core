INSERT INTO dcsa_v1_1.shipment_event (
    event_classifier_code,
    event_type,
    event_date_time,
    event_id,
    event_type_code,
    shipment_information_type_code
    )
    VALUES ('PLN', 'SHIPMENT', '2020-07-15', 'e48f2bc0-c746-11ea-a3ff-db48243a89f4', 'DEPA', 'Some type code text');


INSERT INTO dcsa_v2_0.schedule (
    id,
    vessel_operator_carrier_code,
    vessel_partner_carrier_code,
    start_date,
    date_range,
    vessel_operator_carrier_code_list_provider,
    vessel_partner_carrier_code_list_provider
)
VALUES (
    uuid('35b7b170-c751-11ea-a305-7b347bb9119f'),
    'ZIM',
    'MSK',
    DATE '2020-07-16',
    INTERVAL '3 weeks',
    'SMDG',
    'SMDG'
);

INSERT INTO dcsa_v2_0.transport_call (
    id,
    schedule_id,
    carrier_service_code,
    vessel_imo_number,
    vessel_name,
    carrier_voyage_number,
    un_location_code,
    un_location_name,
    transport_call_number,
    facility_type_code,
    facility_code,
    other_facility
    )
VALUES (
    uuid('8b64d20b-523b-4491-b2e5-32cfa5174eee'),
    uuid('35b7b170-c751-11ea-a305-7b347bb9119f'),
    'Y6S',
    '9466960',
    'NORTHERN JASPER',
    '2007W',
    'ITGOA',
    'Genoa',
    3,
    'TERM',
    'ITGOAASEA',
    NULL);

INSERT INTO dcsa_v2_0.shipment_event (
    id,
    event_classifier_code,
    event_type,
    event_date_time,
    event_type_code,
    shipment_information_type_code,
    transport_call_id
    )
VALUES (
    uuid('784871e7-c9cd-4f59-8d88-2e033fa799a1'),
    'PLN',
    'SHIPMENT',
    '2020-07-15',
    'DEPA',
    'WTF',
    uuid('8b64d20b-523b-4491-b2e5-32cfa5174eee'));
