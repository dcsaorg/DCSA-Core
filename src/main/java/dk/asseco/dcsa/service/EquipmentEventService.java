package dk.asseco.dcsa.service;

import dk.asseco.dcsa.model.EquipmentEvent;
import dk.asseco.dcsa.model.TransportEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EquipmentEventService extends BaseService<EquipmentEvent, String>{

    Flux<EquipmentEvent> findAll();

    Mono<EquipmentEvent> findById(String id);
}
