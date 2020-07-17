package org.dcsa.service;

import org.dcsa.model.Event;
import org.dcsa.model.ShipmentEvent;
import org.dcsa.model.TransportEvent;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransportEventService extends BaseService<TransportEvent, String>{

    Flux<TransportEvent> findAll();

    Mono<TransportEvent> findById(String id);
}
