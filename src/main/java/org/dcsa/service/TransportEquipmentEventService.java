package org.dcsa.service;

import org.dcsa.model.TransportEquipmentEvent;
import org.dcsa.model.TransportEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TransportEquipmentEventService extends BaseService<TransportEquipmentEvent, UUID>{

    Flux<TransportEquipmentEvent> findAll();

    Mono<TransportEquipmentEvent> findById(UUID id);
}
