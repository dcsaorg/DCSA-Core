package org.dcsa.service;

import java.util.List;
import org.dcsa.model.EquipmentEvent;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EquipmentEventService extends BaseService<EquipmentEvent, String>{

    Flux<EquipmentEvent> findAll();

    Mono<EquipmentEvent> findById(String id);

    Flux<EquipmentEvent> findEquipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference);
}
