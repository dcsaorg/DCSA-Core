package dk.asseco.dcsa.service;

import dk.asseco.dcsa.model.Event;
import dk.asseco.dcsa.model.ShipmentEvent;
import dk.asseco.dcsa.model.TransportEvent;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransportEventService extends BaseService<TransportEvent, String>{

    Flux<TransportEvent> findAll();

    Mono<TransportEvent> findById(String id);
}
