package org.dcsa.service;

import org.dcsa.base.service.ExtendedBaseService;
import org.dcsa.model.ShipmentEvent;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface ShipmentEventService extends ExtendedBaseService<ShipmentEvent, UUID> {
    Flux<ShipmentEvent> findShipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference);
}
