package org.dcsa.service;

import org.dcsa.model.EquipmentEvent;
import org.dcsa.model.TransportEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EquipmentEventService extends BaseService<EquipmentEvent, String>{

    Flux<EquipmentEvent> findAll();

    Mono<EquipmentEvent> findById(String id);
}
