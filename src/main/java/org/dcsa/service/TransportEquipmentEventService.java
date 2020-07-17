package org.dcsa.service;

import org.dcsa.model.TransportEquipmentEvent;
import org.dcsa.model.TransportEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransportEquipmentEventService extends BaseService<TransportEquipmentEvent, String>{

    Flux<TransportEquipmentEvent> findAll();

    Mono<TransportEquipmentEvent> findById(String id);
}
