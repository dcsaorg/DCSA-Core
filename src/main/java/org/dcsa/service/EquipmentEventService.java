package org.dcsa.service;

import org.dcsa.base.service.ExtendedBaseService;
import org.dcsa.model.EquipmentEvent;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface EquipmentEventService extends ExtendedBaseService<EquipmentEvent, UUID> {
    Flux<EquipmentEvent> findEquipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference);
}
