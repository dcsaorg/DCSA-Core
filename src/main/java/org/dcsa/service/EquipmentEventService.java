package org.dcsa.service;

import org.dcsa.model.EquipmentEvent;
import org.dcsa.model.TransportEvent;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface EquipmentEventService extends BaseService<EquipmentEvent, UUID>{

    Flux<EquipmentEvent> findAll();

    Mono<EquipmentEvent> findById(UUID id);

    Flux<EquipmentEvent> findEquipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference);
}
