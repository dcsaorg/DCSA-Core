package dk.asseco.dcsa.service;

import dk.asseco.dcsa.model.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventService extends BaseService<Event, String>{

    Flux<Event> findAll();

    Mono<Event> findById(String id);

    <T extends Event> Flux<T> findAllTypes();
}
