package dk.asseco.dcsa.service;

import dk.asseco.dcsa.model.TransportEquipmentEvent;
import dk.asseco.dcsa.model.TransportEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransportEquipmentEventService extends BaseService<TransportEquipmentEvent, String>{

    Flux<TransportEquipmentEvent> findAll();

    Mono<TransportEquipmentEvent> findById(String id);
}
