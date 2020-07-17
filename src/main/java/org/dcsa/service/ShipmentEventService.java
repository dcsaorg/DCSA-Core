package org.dcsa.service;

import org.dcsa.model.Event;
import org.dcsa.model.ShipmentEvent;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ShipmentEventService extends BaseService<ShipmentEvent, String>{
    Flux<ShipmentEvent> findAll();

    Mono<ShipmentEvent> findById(String id);
}
