package org.dcsa.service;

import org.dcsa.model.ShipmentEvent;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface ShipmentEventService extends BaseService<ShipmentEvent, UUID> {
    Flux<ShipmentEvent> findAll();

    Mono<ShipmentEvent> findById(UUID id);

    Flux<ShipmentEvent> findShipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference);
}
