package org.dcsa.service;

import org.dcsa.model.Event;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EventService extends BaseService<Event, String>{

    Flux<Event> findAll();

    Mono<Event> findById(String id);

    <T extends Event> Flux<T> findAllTypes(List<EventType> eventType, String bookingReference, String equipmentReference);

    <T extends Event> Mono<T> saveAll(Event event);
}
