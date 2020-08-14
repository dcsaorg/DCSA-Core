package org.dcsa.service;

import org.dcsa.model.TransportEquipmentEvent;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface TransportEquipmentEventService extends ExtendedBaseService<TransportEquipmentEvent, UUID>{
    Flux<TransportEquipmentEvent> findTransportEquipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference);
}
