package org.dcsa.repository;

import org.dcsa.base.repository.ExtendedRepository;
import org.dcsa.model.ShipmentEvent;
import org.dcsa.model.enums.EventType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ShipmentEventRepository extends ExtendedRepository<ShipmentEvent, UUID> {

    @Query("SELECT * FROM \"dcsa_v1_1\".shipment_event a WHERE (:eventType IS NULL or a.event_type =:eventType)")
    Flux<ShipmentEvent> findShipmentEventsByFilters(@Param("eventType") EventType eventType);
}
